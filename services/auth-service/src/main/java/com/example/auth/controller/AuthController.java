package com.example.auth.controller;

import com.example.auth.service.AuditLogService;
import com.example.auth.service.JwtService;
import com.example.auth.service.RefreshTokenService;
import com.example.auth.service.TokenBlacklist;
import com.example.auth.service.TokenRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // Constants
    private static final String DEFAULT_ROLE = "USER";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long DEFAULT_TOKEN_TTL = 86400000L; // 24 hours

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    
    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceBaseUrl;
    
    @Value("${FRONTEND_BASE_URL:http://localhost:3000}")
    private String frontendBaseUrl;
    
    public AuthController(
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.ttlMillis:86400000}") long ttlMillis,
            @Value("${JWT_SECRET:CHANGE_THIS_TO_A_VERY_LONG_RANDOM_SECRET_KEY_AT_LEAST_256_BITS}") String envSecret,
            @Value("${security.jwt.kid:legacy}") String keyId,
            TokenBlacklist tokenBlacklist,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService
    ) {
        // Prefer explicit security.jwt.secret; fallback to JWT_SECRET env; finally to hardcoded default
        String effective = (secret != null && !secret.isBlank()) ? secret : envSecret;
        if (effective == null || effective.isBlank()) {
            effective = "CHANGE_THIS_TO_A_VERY_LONG_RANDOM_SECRET_KEY_AT_LEAST_256_BITS";
        }
        this.jwtService = new JwtService(effective, ttlMillis, keyId);
        this.tokenBlacklist = tokenBlacklist;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
        // initialize TokenRegistry with blacklist so revokeAllForSubject can revoke tokens
        new TokenRegistry(tokenBlacklist);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> req) {
        String identifier = req.getOrDefault("username", req.getOrDefault("email", ""));
        if (identifier.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username or email required"));
        }
        try {
            RestTemplate rest = new RestTemplate();
            @SuppressWarnings("rawtypes")
            org.springframework.http.ResponseEntity resp;
            try {
                resp = rest.postForEntity(userServiceBaseUrl + "/api/users/login", req, Map.class);
            } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
                int status = httpEx.getStatusCode().value();
                String body = httpEx.getResponseBodyAsString();
                return ResponseEntity.status(status).body(Map.of(
                        "error", httpEx.getStatusText(),
                        "body", body != null ? body : ""
                ));
            }
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

            // Prefer role from login response's user object; fallback to /me
            Map<String, Object> userBody = null;
            String resolvedRole = DEFAULT_ROLE;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> loginBody = (Map<String, Object>) resp.getBody();
                if (loginBody != null && loginBody.get("user") instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) loginBody.get("user");
                    userBody = new java.util.HashMap<>(userMap);
                    Object roleObj = userMap.get("role");
                    if (roleObj != null) {
                        resolvedRole = String.valueOf(roleObj).toUpperCase();
                    }
                }
            } catch (Exception ignored) {}
            // ALWAYS read role from DB via user-service to ensure correctness
            try {
                org.springframework.http.ResponseEntity<Map<String, Object>> meResp = rest.exchange(
                    userServiceBaseUrl + "/api/users/me?identifier=" + identifier,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                if (meResp.getStatusCode().is2xxSuccessful() && meResp.getBody() != null) {
                    Map<String, Object> body = meResp.getBody();
                    if (body != null) {
                        userBody = body;
                        Object roleObj = body.get("role");
                        if (roleObj != null) {
                            resolvedRole = String.valueOf(roleObj).toUpperCase();
                        }
                    }
                }
            } catch (Exception ignored) { }

            // Extract userId from user body for JWT claims111
            String userId = null;
            if (userBody != null && userBody.get("id") != null) {
                userId = userBody.get("id").toString();
            }

            Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("role", resolvedRole);
            if (userId != null) {
                claims.put("userId", userId);
            }

            String token = jwtService.generateToken(identifier, claims);
            TokenRegistry.register(identifier, token, System.currentTimeMillis() + DEFAULT_TOKEN_TTL);
            String refreshToken = refreshTokenService.issue(userId != null ? userId : identifier);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("token", token);
            result.put("tokenType", "Bearer");
            result.put("refreshToken", refreshToken);
            if (userBody != null) {
                // Ensure returned user role matches resolved JWT role
                userBody.put("role", resolvedRole);
                result.put("user", userBody);
            }
            log.info("LOGIN success userId={} role={}", userId, resolvedRole);
            auditLogService.record("LOGIN", userId != null ? userId : identifier, Map.of("role", resolvedRole));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "AUTH-LOGIN-ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> req) {
        String username = req.getOrDefault("username", "");
        String email = req.getOrDefault("email", "");
        String password = req.getOrDefault("password", "");
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username, email, password required"));
        }
        try {
            RestTemplate rest = new RestTemplate();
            var resp = rest.postForEntity(userServiceBaseUrl + "/api/users/register", req, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Register failed"));
            }

            // Get user details to extract userId for JWT claims
            Map<String, Object> claims = new java.util.HashMap<>();
            claims.put("role", DEFAULT_ROLE);

            try {
                org.springframework.http.ResponseEntity<Map<String, Object>> meResp = rest.exchange(
                    userServiceBaseUrl + "/api/users/me?identifier=" + username,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                if (meResp.getStatusCode().is2xxSuccessful() && meResp.getBody() != null) {
                    Map<String, Object> userBody = meResp.getBody();
                    if (userBody != null) {
                        Object idObj = userBody.get("id");
                        if (idObj != null) {
                            String userId = idObj.toString();
                            claims.put("userId", userId);
                        }
                    }
                }
            } catch (Exception ignored) { }

            String token = jwtService.generateToken(username, claims);
            String refreshToken = refreshTokenService.issue(username);
            auditLogService.record("REGISTER", username, Map.of());
            return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", BEARER_PREFIX.trim(),
                "refreshToken", refreshToken
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send OTP to phone for registration/login
     */
    @PostMapping("/phone/send-otp")
    public ResponseEntity<Map<String, Object>> sendPhoneOtp(@RequestBody Map<String, String> body) {
        try {
            RestTemplate rest = new RestTemplate();
            var resp = rest.postForEntity(userServiceBaseUrl + "/api/users/phone/send-otp", body, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) resp.getBody();
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Verify phone with OTP and create/login user
     */
    @PostMapping("/phone/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyPhoneOtp(@RequestBody Map<String, String> body) {
        try {
            RestTemplate rest = new RestTemplate();
            var resp = rest.postForEntity(userServiceBaseUrl + "/api/users/phone/verify-otp", body, Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = (Map<String, Object>) resp.getBody();
                if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                    String username = body.get("username");

                    // Extract userId for JWT claims
                    Map<String, Object> claims = new java.util.HashMap<>();
                    claims.put("role", "USER");

                    if (response.get("user") instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> user = (Map<String, Object>) response.get("user");
                        if (user.get("id") != null) {
                            String userId = user.get("id").toString();
                            claims.put("userId", userId);
                        }
                    }

                    String token = jwtService.generateToken(username, claims);
                    String refreshToken = refreshTokenService.issue(username);
                    response.put("token", token);
                    response.put("tokenType", "Bearer");
                    response.put("refreshToken", refreshToken);
                    auditLogService.record("PHONE_VERIFY", username, Map.of());
                }
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "OTP verification failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Forgot password - Send reset email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            RestTemplate rest = new RestTemplate();
            var resp = rest.postForEntity(userServiceBaseUrl + "/api/users/password/forgot", request, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) resp.getBody();
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            // user-service expects { token, newPassword }
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("token", request.getOrDefault("token", ""));
            // frontend sends 'password', map it to 'newPassword'
            String newPwd = request.getOrDefault("newPassword", request.getOrDefault("password", ""));
            payload.put("newPassword", newPwd);

            RestTemplate rest = new RestTemplate();
            org.springframework.http.ResponseEntity<Map<String, Object>> resp;
            try {
                resp = rest.exchange(
                    userServiceBaseUrl + "/api/users/password/reset",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(payload),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
            } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
                int status = httpEx.getStatusCode().value();
                String body = httpEx.getResponseBodyAsString();
                return ResponseEntity.status(status).body(java.util.Map.of(
                    "success", false,
                    "error", httpEx.getStatusText(),
                    "body", body != null ? body : ""
                ));
            }
            Map<String, Object> responseBody = resp.getBody();
            if (responseBody == null) {
                return ResponseEntity.status(resp.getStatusCode()).body(Map.of("success", false, "error", "No response body"));
            }
            return ResponseEntity.status(resp.getStatusCode()).body(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Email verification
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            RestTemplate rest = new RestTemplate();
            org.springframework.http.ResponseEntity<Map<String, Object>> resp;
            try {
                resp = rest.exchange(
                    userServiceBaseUrl + "/api/users/verify-email",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(request),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
            } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
                int status = httpEx.getStatusCode().value();
                String body = httpEx.getResponseBodyAsString();
                return ResponseEntity.status(status).body(java.util.Map.of(
                    "success", false,
                    "error", httpEx.getStatusText(),
                    "body", body != null ? body : ""
                ));
            }
            Map<String, Object> responseBody = resp.getBody();
            if (responseBody == null) {
                return ResponseEntity.status(resp.getStatusCode()).body(Map.of("success", false, "error", "No response body"));
            }
            return ResponseEntity.status(resp.getStatusCode()).body(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Change password (authenticated user)
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace(BEARER_PREFIX, "");
            String subject = jwtService.parseSubject(token);
            
            RestTemplate rest = new RestTemplate();
            var resp = rest.postForEntity(userServiceBaseUrl + "/api/users/change-password?userId=" + subject, request, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) resp.getBody();
            // Revoke all tokens after password change
            tokenBlacklist.revoke(token, System.currentTimeMillis() + 3600000);
            TokenRegistry.revokeAllForSubject(subject);
            refreshTokenService.revokeAllForUser(subject);
            log.info("PASSWORD CHANGE revoke tokens for user {}", subject);
        auditLogService.record("PASSWORD_CHANGE", subject, Map.of());
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * OAuth2 signup
     */
    @PostMapping("/oauth2-signup")
    public ResponseEntity<Map<String, Object>> oauth2Signup(@RequestBody Map<String, String> req) {
        try {
            RestTemplate rest = new RestTemplate();
            org.springframework.http.ResponseEntity<Map<String, Object>> resp = rest.exchange(
                userServiceBaseUrl + "/api/users/oauth2-signup",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(req),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (resp.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> response = resp.getBody();
                if (response != null) {
                    String username = req.get("username");

                    // Extract userId for JWT claims
                    Map<String, Object> claims = new java.util.HashMap<>();
                    claims.put("role", "USER");

                    Object userObj = response.get("user");
                    if (userObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> user = (Map<String, Object>) userObj;
                        Object idObj = user.get("id");
                        if (idObj != null) {
                            String userId = idObj.toString();
                            claims.put("userId", userId);
                        }
                    }

                    String token = jwtService.generateToken(username, claims);
                    response.put("token", token);
                    response.put("tokenType", "Bearer");
                }
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "OAuth2 signup failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String token = auth.substring(BEARER_PREFIX.length());
        if (tokenBlacklist.isRevoked(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Token revoked"));
        }
        String subject;
        try {
            subject = jwtService.parseSubject(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
        try {
            RestTemplate rest = new RestTemplate();
            org.springframework.http.ResponseEntity<Map<String, Object>> resp = rest.exchange(
                userServiceBaseUrl + "/api/users/me?identifier=" + subject,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            Map<String, Object> body = resp.getBody();
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing token"));
        }
        String token = auth.substring(BEARER_PREFIX.length());
        try {
            var claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(jwtService.getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            long exp = claims.getExpiration() != null ? claims.getExpiration().getTime() : System.currentTimeMillis() + 3600000;
            tokenBlacklist.revoke(token, exp);
            Object uid = claims.get("userId");
            if (uid != null) {
                refreshTokenService.revokeAllForUser(uid.toString());
                TokenRegistry.revokeAllForSubject(uid.toString());
            } else {
                TokenRegistry.revokeAllForSubject(claims.getSubject());
            }
            log.info("LOGOUT userId={} sub={}", claims.get("userId"), claims.getSubject());
            auditLogService.record("LOGOUT", claims.get("userId") != null ? claims.get("userId").toString() : claims.getSubject(), Map.of());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "error", "Invalid token"));
        }
    }

    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspect(@RequestBody Map<String, String> body) {
        String token = String.valueOf(body.getOrDefault("token", ""));
        if (token.isBlank()) return ResponseEntity.badRequest().body(Map.of("active", false));
        if (tokenBlacklist.isRevoked(token)) return ResponseEntity.ok(Map.of("active", false));
        try {
            var claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(jwtService.getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String sub = claims.getSubject();
            Object role = claims.get("role");
            return ResponseEntity.ok(Map.of("active", true, "sub", sub, "role", role));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("active", false));
        }
    }

    @PostMapping("/admin/revoke-user/{subject}")
    public ResponseEntity<Map<String, Object>> revokeUser(@PathVariable("subject") String subject) {
        TokenRegistry.revokeAllForSubject(subject);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Refresh token rotation endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing refreshToken"));
        }
        String userId = refreshTokenService.validateAndRotate(refreshToken);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Invalid refresh token"));
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        String access = jwtService.generateToken(userId, claims);
        TokenRegistry.register(userId, access, System.currentTimeMillis() + DEFAULT_TOKEN_TTL);
        String newRefresh = refreshTokenService.issue(userId);
        log.info("REFRESH success userId={}", userId);
        auditLogService.record("REFRESH", userId, Map.of());
        return ResponseEntity.ok(Map.of(
            "success", true,
            "token", access,
            "tokenType", "Bearer",
            "refreshToken", newRefresh
        ));
    }

    /**
     * OAuth2 Success Handler (Google & Facebook)
     * Redirects to frontend with token in query parameter
     */
    @GetMapping("/oauth2-success")
    public void oauth2Success(Authentication authentication, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        try {
            if (authentication != null && authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                
                // Detect provider (Google or Facebook)
                String provider = "GOOGLE"; // default
                String email = oauth2User.getAttribute("email");
                String name = oauth2User.getAttribute("name");
                String picture = null;
                String providerId = null;
                
                // Check if Facebook (has 'id' attribute without 'sub')
                if (oauth2User.getAttribute("id") != null && oauth2User.getAttribute("sub") == null) {
                    provider = "FACEBOOK";
                    providerId = oauth2User.getAttribute("id");
                    
                    // Facebook picture structure: {data: {url: "..."}}
                    Object pictureObj = oauth2User.getAttribute("picture");
                    if (pictureObj instanceof Map) {
                        Object data = ((Map<?, ?>) pictureObj).get("data");
                        if (data instanceof Map) {
                            picture = (String) ((Map<?, ?>) data).get("url");
                        }
                    }
                } else {
                    // Google
                    provider = "GOOGLE";
                    providerId = oauth2User.getAttribute("sub");
                    picture = oauth2User.getAttribute("picture");
                }
                
                if (email == null || email.isEmpty()) {
                    String errorUrl = frontendBaseUrl + "/oauth2-success?error=" + 
                        java.net.URLEncoder.encode("Email not provided by " + provider, "UTF-8");
                    response.sendRedirect(errorUrl);
                    return;
                }
                
                // Create or update user via user service
                Map<String, Object> userData = new java.util.HashMap<>();
                userData.put("email", email);
                userData.put("username", email.split("@")[0]);
                userData.put("firstName", name != null ? name.split(" ")[0] : "");
                userData.put("lastName", name != null && name.contains(" ") ? name.substring(name.indexOf(" ")).trim() : "");
                userData.put("profilePicture", picture != null ? picture : "");
                userData.put("provider", provider);
                
                if ("GOOGLE".equals(provider)) {
                    userData.put("googleId", providerId);
                } else if ("FACEBOOK".equals(provider)) {
                    userData.put("facebookId", providerId);
                }
                
                RestTemplate rest = new RestTemplate();
                org.springframework.http.ResponseEntity<Map<String, Object>> userResp = rest.exchange(
                    userServiceBaseUrl + "/api/users/oauth2-signup",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(userData),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                if (userResp.getStatusCode().is2xxSuccessful() && userResp.getBody() != null) {
                    Map<String, Object> user = userResp.getBody();
                    
                    if (user == null) {
                        String errorUrl = frontendBaseUrl + "/oauth2-success?error=" + 
                            java.net.URLEncoder.encode("Failed to get user data", "UTF-8");
                        response.sendRedirect(errorUrl);
                        return;
                    }

                    // Extract userId for JWT claims
                    Map<String, Object> claims = new java.util.HashMap<>();
                    claims.put("role", "USER");

                    Object userIdObj = user.get("id");
                    if (userIdObj != null) {
                        String userId = userIdObj.toString();
                        claims.put("userId", userId);
                    }

                    // Generate JWT token
                    String token = jwtService.generateToken(email, claims);
                    TokenRegistry.register(email, token, System.currentTimeMillis() + 86400000);
                    String refreshToken = refreshTokenService.issue(email);
                    
                    // Redirect to frontend with token
                    String redirectUrl = frontendBaseUrl + "/oauth2-success?token=" + 
                        java.net.URLEncoder.encode(token, "UTF-8") +
                        "&refreshToken=" + java.net.URLEncoder.encode(refreshToken, "UTF-8") +
                        "&provider=" + java.net.URLEncoder.encode(provider, "UTF-8");
                    response.sendRedirect(redirectUrl);
                    auditLogService.record("OAUTH2_LOGIN", email, Map.of("provider", provider));
                } else {
                    String errorUrl = frontendBaseUrl + "/oauth2-success?error=" + 
                        java.net.URLEncoder.encode("Failed to create/update user", "UTF-8");
                    response.sendRedirect(errorUrl);
                }
            } else {
                String errorUrl = frontendBaseUrl + "/oauth2-success?error=" + 
                    java.net.URLEncoder.encode("OAuth2 authentication failed", "UTF-8");
                response.sendRedirect(errorUrl);
            }
        } catch (Exception e) {
            String errorUrl = "http://localhost:3000/oauth2-success?error=" + 
                java.net.URLEncoder.encode("OAuth2 success handler error: " + e.getMessage(), "UTF-8");
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * OAuth2 Failure Handler
     */
    @GetMapping("/oauth2-failure")
    public ResponseEntity<Map<String, Object>> oauth2Failure() {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "error", "Google OAuth2 authentication failed",
            "message", "Please try again or use email/password login"
        ));
    }
}


