package com.example.user.controller;

import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.service.UserService;
import com.example.user.service.PasswordResetService;
import com.example.shared.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.CompletableFuture;
    import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.annotation.security.PermitAll;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${file.upload-dir:D:/Buildd24_10/Buildd30_7/Buildd43/services/user-service/uploads}")
    private String uploadDir;

    @GetMapping("/health")
    @PermitAll
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    /**
     * Reset password
     */
    @PostMapping("/password/reset")
    @PermitAll
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.getOrDefault("token", "");
            String newPassword = request.getOrDefault("newPassword", request.getOrDefault("password", ""));
            if (token.isEmpty() || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Token and new password are required"));
            }

            boolean ok = passwordResetService.resetPassword(token, newPassword);
            if (ok) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully"));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid or expired token"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * User registration
     */
    @PostMapping("/register")
    @PermitAll
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        try {
            String username = body.getOrDefault("username", "");
            String email = body.getOrDefault("email", "");
            String password = body.getOrDefault("password", "");
            String firstName = body.getOrDefault("firstName", "");
            String lastName = body.getOrDefault("lastName", "");
            String phoneNumber = body.getOrDefault("phoneNumber", "");

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Username, email, and password are required"));
            }

            // Check if user already exists
            if (userService.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Username already exists"));
            }
            if (userService.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email already exists"));
            }

            User user = userService.createUser(username, email, password, firstName, lastName, phoneNumber);

            // Fire-and-forget email trigger with short timeouts to avoid blocking registration
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
                    f.setConnectTimeout(5000);
                    f.setReadTimeout(5000);
                    RestTemplate rest = new RestTemplate(f);
                    Map<String, String> payload = new HashMap<>();
                    payload.put("email", user.getEmail());
                    payload.put("token", user.getEmailVerificationToken());
                    payload.put("baseUrl", frontendBaseUrl);
                    rest.postForEntity(notificationServiceBaseUrl + "/api/notifications/email/verify", payload, Map.class);
                } catch (Exception ignored) {
                }
            });

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful. Please check your email for verification.");
            response.put("user", createUserResponse(user));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Registration failed: " + e.getMessage()));
        }
    }

    /**
     * User login
     */
    @PostMapping("/login")
    @PermitAll
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            String usernameOrEmail = body.getOrDefault("username", body.getOrDefault("email", ""));
            String password = body.getOrDefault("password", "");

            if (usernameOrEmail.isEmpty() || password.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Username/email and password are required"));
            }

            var userOpt = userService.authenticateUser(usernameOrEmail, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
                    return ResponseEntity.status(403).body(Map.of("success", false, "error", "Email not verified. Please check your email to verify your account."));
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("user", createUserResponse(user));
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("success", false, "error", "Invalid username/email or password"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Login failed: " + e.getMessage()));
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    @PermitAll
    public ResponseEntity<Map<String, Object>> me(@RequestParam String identifier) {
        try {
            var userOpt = userService.findByUsernameOrEmail(identifier);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(createUserResponse(userOpt.get()));
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user profile (alias for /me endpoint)
     */
    @GetMapping("/profile")
    @PermitAll
    public ResponseEntity<Map<String, Object>> profile(@RequestParam(required = false) String identifier) {
        try {
            if (identifier == null || identifier.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User identifier is required"));
            }
            
            var userOpt = userService.findByUsernameOrEmail(identifier);
            if (userOpt.isEmpty() && SecurityUtils.isValidUUID(identifier)) {
                userOpt = userService.findById(identifier);
            }
            
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(createUserResponse(userOpt.get()));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        try {
            var userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(createUserResponse(userOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        try {
            var userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(createUserResponse(userOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        try {
            var userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(createUserResponse(userOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send OTP to phone for registration/login
     */
    @PostMapping("/phone/send-otp")
    @PermitAll
    public ResponseEntity<Map<String, Object>> sendPhoneOtp(@RequestBody Map<String, String> body) {
        try {
            String phone = body.getOrDefault("phone", "");
            if (phone.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số điện thoại không hợp lệ"));
            }
            
            boolean success = userService.sendPhoneOtp(phone);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi OTP"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể gửi OTP"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Verify phone with OTP and create/login user
     */
    @PostMapping("/phone/verify-otp")
    @PermitAll
    public ResponseEntity<Map<String, Object>> verifyPhoneOtp(@RequestBody Map<String, String> body) {
        try {
            String phone = body.getOrDefault("phone", "");
            String otp = body.getOrDefault("otp", "");
            String username = body.getOrDefault("username", "");
            String password = body.getOrDefault("password", "");
            String firstName = body.getOrDefault("firstName", "");
            String lastName = body.getOrDefault("lastName", "");
            String email = body.getOrDefault("email", "");

            if (phone.isEmpty() || otp.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu số điện thoại hoặc OTP"));
            }

            var result = userService.verifyPhoneOtp(phone, otp, username, password, firstName, lastName, email);
            if (result.get("success").equals(true)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * OAuth2 signup (Google & Facebook)
     */
    @PostMapping("/oauth2-signup")
    @PermitAll
    public ResponseEntity<Map<String, Object>> oauth2Signup(@RequestBody Map<String, Object> req) {
        try {
            String email = (String) req.getOrDefault("email", "");
            String username = (String) req.getOrDefault("username", "");
            String firstName = (String) req.getOrDefault("firstName", "");
            String lastName = (String) req.getOrDefault("lastName", "");
            String profilePicture = (String) req.getOrDefault("profilePicture", "");
            String provider = (String) req.getOrDefault("provider", "GOOGLE");
            String googleId = (String) req.get("googleId");
            String facebookId = (String) req.get("facebookId");
            
            if (email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
            }
            
            // Check if user exists by email
            User user = userService.findByEmail(email).orElse(null);
            
            if (user == null) {
                // Create new user
                user = new User();
                user.setEmail(email);
                user.setUsername(username.isEmpty() ? email.split("@")[0] : username);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setProfileImageUrl(profilePicture);
                user.setIsActive(true);
                user.setIsEmailVerified(true); // OAuth emails are pre-verified
                user.setRole(UserRole.USER);
            }
            
            // Update OAuth provider info
            user.setProvider(provider);
            if ("GOOGLE".equals(provider) && googleId != null) {
                user.setGoogleId(googleId);
            } else if ("FACEBOOK".equals(provider) && facebookId != null) {
                user.setFacebookId(facebookId);
            }
            
            // Update profile picture if provided
            if (profilePicture != null && !profilePicture.isEmpty()) {
                user.setProfileImageUrl(profilePicture);
            }
            
            // Save user
            user = userService.save(user);
            
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "profilePicture", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "",
                "provider", user.getProvider() != null ? user.getProvider() : "",
                "role", user.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    @PreAuthorize("#userId == authentication.principal?.id or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestParam String userId,
                                                             @RequestBody Map<String, Object> profileData) {
        try {
            User updated = userService.updateUserProfile(userId, profileData);
            if (updated == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", createUserResponse(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Update user preferences
     */
    @PutMapping("/preferences")
    @PreAuthorize("#userId == authentication.principal?.id or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePreferences(@RequestParam String userId,
                                                                 @RequestBody Map<String, Object> preferencesData) {
        try {
            User updated = userService.updateUserPreferences(userId, preferencesData);
            if (updated == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Preferences updated successfully");
            response.put("user", createUserResponse(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Update notification settings
     */
    @PutMapping("/notifications")
    @PreAuthorize("#userId == authentication.principal?.id or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateNotificationSettings(@RequestParam String userId,
                                                                          @RequestBody Map<String, Object> notificationData) {
        try {
            User updated = userService.updateNotificationSettings(userId, notificationData);
            if (updated == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification settings updated successfully");
            response.put("user", createUserResponse(updated));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    @PreAuthorize("#userId == authentication.principal?.id")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestParam String userId,
                                                              @RequestBody Map<String, String> payload) {
        try {
            String currentPassword = payload.get("currentPassword");
            String newPassword = payload.get("newPassword");
            
            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Current password and new password are required"));
            }
            
            boolean success = userService.changePassword(userId, currentPassword, newPassword);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Current password is incorrect"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Upload avatar image
     */
    @PostMapping("/avatar/upload")
    @PermitAll
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam String userId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File must be an image"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "File size must be less than 5MB"));
            }

            // Create upload directory if it doesn't exist
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;
            Path filePath = Paths.get(uploadDir, filename);

            // Save file
            Files.write(filePath, file.getBytes());

            // Generate URL (relative path for frontend to access)
            // Use /api/uploads/ to match the endpoint in StaticResourceController
            String avatarUrl = "/api/uploads/" + filename;

            // Update user profile image URL
            var userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setProfileImageUrl(avatarUrl);
                userService.save(user);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Avatar uploaded successfully");
                response.put("avatarUrl", avatarUrl);
                response.put("user", createUserResponse(user));
                return ResponseEntity.ok(response);
            } else {
                // Delete uploaded file if user not found
                Files.deleteIfExists(filePath);
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }


    /**
     * Get all users (admin only)
     */
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> listAll(Pageable pageable) {
        try {
            Page<User> users = userService.findAll(pageable);
            Page<Map<String, Object>> userResponses = users.map(this::createUserResponse);
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete user (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> softDelete(@PathVariable String id) {
        try {
            var userOpt = userService.findById(id);
            if (userOpt.isEmpty()) return ResponseEntity.notFound().build();
            User u = userOpt.get();
            u.setIsDeleted(true);
            u.setIsActive(false);
            userService.save(u);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Change user role (admin only)
     */
    @PostMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> changeRole(@PathVariable String id, 
                                                          @RequestBody Map<String, String> payload) {
        try {
            String role = payload.get("role");
            String reason = payload.getOrDefault("reason", "Role changed via API");
            String changedBy = payload.getOrDefault("changedBy", "admin");
            
            if (role == null || role.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Role is required"));
            }
            
            UserRole userRole;
            try {
                userRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid role: " + role));
            }
            
            boolean success = userService.changeUserRole(id, userRole, reason, changedBy);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "User role updated successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Change user status (admin only)
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable String id, 
                                                            @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean isActive = payload.get("isActive");
            if (isActive == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "isActive is required"));
            }
            
            boolean success = userService.toggleUserStatus(id, isActive);
            if (success) {
                String message = isActive ? "User activated successfully" : "User deactivated successfully";
                return ResponseEntity.ok(Map.of("success", true, "message", message));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Unlock user (admin only)
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unlockUser(@PathVariable String id) {
        try {
            userService.unlockUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "User unlocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get user statistics (admin only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            List<User> allUsers = userService.getAllUsers();
            
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream().mapToLong(u -> Boolean.TRUE.equals(u.getIsActive()) ? 1 : 0).sum();
            long verifiedUsers = allUsers.stream().mapToLong(u -> Boolean.TRUE.equals(u.getIsEmailVerified()) ? 1 : 0).sum();
            long lockedUsers = allUsers.stream().mapToLong(u -> userService.isUserLocked(u) ? 1 : 0).sum();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers);
            stats.put("verifiedUsers", verifiedUsers);
            stats.put("lockedUsers", lockedUsers);
            stats.put("inactiveUsers", totalUsers - activeUsers);
            stats.put("unverifiedUsers", totalUsers - verifiedUsers);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get users by role (admin only)
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getUsersByRole(@PathVariable String role) {
        try {
            com.example.user.entity.UserRole userRole;
            try {
                userRole = com.example.user.entity.UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
            
            List<User> users = userService.getUsersByRole(userRole);
            List<Map<String, Object>> userResponses = users.stream()
                    .map(this::createUserResponse)
                    .toList();
            
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Forgot password - Send reset email
     */
    @PostMapping("/password/forgot")
    @PermitAll
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.getOrDefault("email", "");
            if (email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email is required"));
            }

            // Generate reset token
            String resetToken = passwordResetService.requestReset(email);
            if (resetToken == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found with this email"));
            }

            // Send reset email via notification service
            CompletableFuture.runAsync(() -> {
                try {
                    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
                    f.setConnectTimeout(5000);
                    f.setReadTimeout(5000);
                    RestTemplate rest = new RestTemplate(f);
                    Map<String, String> payload = new HashMap<>();
                    payload.put("email", email);
                    payload.put("token", resetToken);
                    payload.put("baseUrl", frontendBaseUrl);
                    rest.postForEntity(notificationServiceBaseUrl + "/api/notifications/email/password-reset", payload, Map.class);
                } catch (Exception ignored) {
                    // Log error but don't block the response
                }
            });

            return ResponseEntity.ok(Map.of("success", true, "message", "Password reset email sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to process request: " + e.getMessage()));
        }
    }

    /**
     * Verify email with token
     */
    @PostMapping("/verify-email")
    @PermitAll
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody Map<String, String> request) {
        try {
            String token = request.getOrDefault("token", "");
            if (token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Token is required"));
            }

            User verifiedUser = userService.verifyEmailAndGetUser(token);
            if (verifiedUser != null) {
                // Send welcome email after successful verification
                CompletableFuture.runAsync(() -> {
                    sendWelcomeEmail(
                        verifiedUser.getEmail(),
                        verifiedUser.getFirstName(),
                        verifiedUser.getLastName()
                    );
                });

                return ResponseEntity.ok(Map.of("success", true, "message", "Email verified successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid or expired token"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to verify email: " + e.getMessage()));
        }
    }

    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    @PermitAll
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(@RequestBody Map<String, String> request) {
        try {
            String email = request.getOrDefault("email", "");
            if (email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email is required"));
            }

            // Find user by email
            var userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }

            User user = userOpt.get();

            // Check if already verified
            if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email already verified"));
            }

            // Generate new verification token
            String newToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(newToken);
            user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            userService.save(user);

            // Send verification email via notification service (with retry)
            CompletableFuture.runAsync(() -> {
                try {
                    sendVerificationEmailWithRetry(user.getEmail(), newToken, frontendBaseUrl, 3);
                } catch (Exception ignored) {
                    // Log error but don't block the response
                }
            });

            return ResponseEntity.ok(Map.of("success", true, "message", "Verification email sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to resend verification email: " + e.getMessage()));
        }
    }

    /**
     * Helper method to send welcome email
     */
    private void sendWelcomeEmail(String email, String firstName, String lastName) {
        try {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(5000);
            f.setReadTimeout(5000);
            RestTemplate rest = new RestTemplate(f);

            Map<String, String> payload = new HashMap<>();
            payload.put("email", email);
            payload.put("firstName", firstName != null ? firstName : "");
            payload.put("lastName", lastName != null ? lastName : "");

            rest.postForEntity(notificationServiceBaseUrl + "/api/notifications/email/welcome", payload, Map.class);
        } catch (Exception ignored) {
            // Log error but don't block the response
        }
    }

    /**
     * Helper method to send verification email with retry logic
     */
    private void sendVerificationEmailWithRetry(String email, String token, String baseUrl, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
                f.setConnectTimeout(5000);
                f.setReadTimeout(5000);
                RestTemplate rest = new RestTemplate(f);

                Map<String, String> payload = new HashMap<>();
                payload.put("email", email);
                payload.put("token", token);
                payload.put("baseUrl", baseUrl);

                rest.postForEntity(notificationServiceBaseUrl + "/api/notifications/email/verify", payload, Map.class);
                return; // Success, exit
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    // Log final failure
                    System.err.println("Failed to send verification email after " + maxRetries + " attempts: " + e.getMessage());
                } else {
                    // Exponential backoff: 1s, 2s, 4s
                    try {
                        Thread.sleep(1000 * (1L << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }


    /**
     * Create user response DTO
     */
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("address", user.getAddress());
        response.put("city", user.getCity());
        response.put("postalCode", user.getPostalCode());
        response.put("country", user.getCountry());
        response.put("dateOfBirth", user.getDateOfBirth());
        response.put("role", user.getRole() != null ? user.getRole().name() : null);
        response.put("isActive", user.getIsActive());
        response.put("isDeleted", user.getIsDeleted());
        response.put("isEmailVerified", user.getIsEmailVerified());
        response.put("personalizationEnabled", user.getPersonalizationEnabled());
        response.put("chatbotEnabled", user.getChatbotEnabled());
        response.put("recommendationEnabled", user.getRecommendationEnabled());
        response.put("lastLoginAt", user.getLastLoginAt());
        response.put("createdAt", user.getCreatedAt());
        response.put("updatedAt", user.getUpdatedAt());
        response.put("isLocked", userService.isUserLocked(user));
        response.put("avatarUrl", user.getProfileImageUrl());
        response.put("profileImageUrl", user.getProfileImageUrl());

        return response;
    }
}