package com.example.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${services.auth.base-url:http://localhost:8081}")
    private String authServiceBaseUrl;

    private final RestTemplate rest = new RestTemplate();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Allow CORS preflight requests to pass through without auth
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/api/admin/health")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = authHeader.substring("Bearer ".length());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("token", token), headers);
            org.springframework.http.ResponseEntity<?> resp = rest.postForEntity(authServiceBaseUrl + "/api/auth/introspect", req, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) resp.getBody();
            if (body == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            Object active = body.get("active");
            if (!(active instanceof Boolean) || !((Boolean) active)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            Object role = body.get("role");
            String roleStr = role != null ? String.valueOf(role) : null;
            if (roleStr == null || !("ADMIN".equalsIgnoreCase(roleStr) || "ROLE_ADMIN".equalsIgnoreCase(roleStr))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // Mark request as authenticated with ADMIN authority so Spring Security passes .authenticated()
            String subject = body.get("sub") != null ? String.valueOf(body.get("sub")) : "admin";
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}


