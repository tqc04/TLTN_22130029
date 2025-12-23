package com.example.user.service;

import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceBaseUrl;

    /**
     * Authenticate user with username/email and password
     */
    public Optional<User> authenticateUser(String usernameOrEmail, String password) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Check if user is active and password matches
            if (user.getIsActive() && passwordEncoder.matches(password, user.getPassword())) {
                // Update last login
                user.setLastLoginAt(LocalDateTime.now());
                user.setLoginAttempts(0);
                userRepository.save(user);
                return Optional.of(user);
            } else {
                // Increment login attempts
                user.setLoginAttempts(user.getLoginAttempts() + 1);
                if (user.getLoginAttempts() >= 5) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                }
                userRepository.save(user);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Update last login time
     */
    public void updateLastLogin(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Create new user
     */
    public User createUser(String username, String email, String password, String firstName, String lastName, String phoneNumber) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setPersonalizationEnabled(true);
        user.setRecommendationEnabled(true);
        user.setChatbotEnabled(true);

        // Generate email verification token
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hour expiry

        return userRepository.save(user);
    }
    
    /**
     * Register user (legacy method)
     */
    public User register(String username, String email, String password) {
        return createUser(username, email, password, null, null, null);
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Find user by username or email (optimized single query)
     */
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    /**
     * Find user by phone number
     */
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }
    
    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Generate password reset token
     */
    public String generatePasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1)); // 1 hour expiry
            userRepository.save(user);
            return token;
        }
        return null;
    }
    
    /**
     * Reset password using token
     */
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByPasswordResetToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Check if token is still valid
            if (user.getPasswordResetTokenExpiresAt() != null &&
                user.getPasswordResetTokenExpiresAt().isAfter(LocalDateTime.now())) {

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setPasswordResetToken(null);
                user.setPasswordResetTokenExpiresAt(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Find user by email verification token
     */
    public Optional<User> findByEmailVerificationToken(String token) {
        return userRepository.findByEmailVerificationToken(token);
    }

    /**
     * Verify email using token
     */
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isEmailVerificationTokenValid()) {
                user.setIsEmailVerified(true);
                user.setEmailVerificationToken(null);
                user.setEmailVerificationTokenExpiresAt(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    /**
     * Verify email and return updated user if success
     */
    public User verifyEmailAndGetUser(String token) {
        Optional<User> userOpt = userRepository.findByEmailVerificationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isEmailVerificationTokenValid()) {
                user.setIsEmailVerified(true);
                user.setEmailVerificationToken(null);
                user.setEmailVerificationTokenExpiresAt(null);
                return userRepository.save(user);
            }
        }
        return null;
    }

    /**
     * Regenerate email verification token for a given email
     */
    public User regenerateEmailVerificationToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                return null; // already verified
            }
            user.setEmailVerificationToken(java.util.UUID.randomUUID().toString());
            user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
            return userRepository.save(user);
        }
        return null;
    }

    /**
     * Get all users (admin function)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Get user by ID
     */
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }
    
    /**
     * Update user profile
     */
    public User updateUserProfile(String userId, Map<String, Object> profileData) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (profileData.containsKey("firstName")) {
                user.setFirstName((String) profileData.get("firstName"));
            }
            if (profileData.containsKey("lastName")) {
                user.setLastName((String) profileData.get("lastName"));
            }
            if (profileData.containsKey("phoneNumber")) {
                user.setPhoneNumber((String) profileData.get("phoneNumber"));
            }
            if (profileData.containsKey("address")) {
                user.setAddress((String) profileData.get("address"));
            }
            if (profileData.containsKey("city")) {
                user.setCity((String) profileData.get("city"));
            }
            if (profileData.containsKey("postalCode")) {
                user.setPostalCode((String) profileData.get("postalCode"));
            }
            if (profileData.containsKey("country")) {
                user.setCountry((String) profileData.get("country"));
            }
            if (profileData.containsKey("dateOfBirth")) {
                String dateOfBirthStr = (String) profileData.get("dateOfBirth");
                if (dateOfBirthStr != null && !dateOfBirthStr.isEmpty()) {
                    try {
                        LocalDateTime dateOfBirth = LocalDateTime.parse(dateOfBirthStr + "T00:00:00");
                        user.setDateOfBirth(dateOfBirth);
                    } catch (Exception e) {
                        logger.warn("Failed to parse dateOfBirth: {}", dateOfBirthStr);
                    }
                }
            }
            if (profileData.containsKey("email")) {
                String newEmail = (String) profileData.get("email");
                if (newEmail != null && !newEmail.equals(user.getEmail())) {
                    // Check if new email already exists for another user
                    Optional<User> existingUser = userRepository.findByEmail(newEmail);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                        throw new RuntimeException("Email already exists: " + newEmail);
                    }
                    user.setEmail(newEmail);
                    user.setIsEmailVerified(false);
                }
            }
            
            return userRepository.save(user);
        }
        return null;
    }

    /**
     * Update user preferences (personalization, chatbot, recommendations)
     */
    public User updateUserPreferences(String userId, Map<String, Object> preferencesData) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (preferencesData.containsKey("personalizationEnabled")) {
                user.setPersonalizationEnabled((Boolean) preferencesData.get("personalizationEnabled"));
            }
            if (preferencesData.containsKey("chatbotEnabled")) {
                user.setChatbotEnabled((Boolean) preferencesData.get("chatbotEnabled"));
            }
            if (preferencesData.containsKey("recommendationEnabled")) {
                user.setRecommendationEnabled((Boolean) preferencesData.get("recommendationEnabled"));
            }
            
            return userRepository.save(user);
        }
        return null;
    }

    /**
     * Update user notification settings
     */
    public User updateNotificationSettings(String userId, Map<String, Object> notificationData) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Create notification settings JSON
            Map<String, Boolean> notificationSettings = new HashMap<>();
            notificationSettings.put("emailNotifications", (Boolean) notificationData.getOrDefault("emailNotifications", true));
            notificationSettings.put("pushNotifications", (Boolean) notificationData.getOrDefault("pushNotifications", false));
            notificationSettings.put("orderUpdates", (Boolean) notificationData.getOrDefault("orderUpdates", true));
            notificationSettings.put("promotionalEmails", (Boolean) notificationData.getOrDefault("promotionalEmails", true));
            notificationSettings.put("securityAlerts", (Boolean) notificationData.getOrDefault("securityAlerts", true));
            
            // Convert to JSON string
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String notificationSettingsJson = objectMapper.writeValueAsString(notificationSettings);
                user.setNotificationSettings(notificationSettingsJson);
            } catch (Exception e) {
                logger.warn("Failed to serialize notification settings: {}", e.getMessage());
            }
            
            return userRepository.save(user);
        }
        return null;
    }

    /**
     * Change user password
     */
    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Verify current password
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
            }
        }
        return false;
    }

    /**
     * Activate or deactivate user
     */
    public boolean toggleUserStatus(String userId, boolean isActive) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIsActive(isActive);
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    /**
     * Find all users with pagination
     */
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findByIsDeletedFalse(pageable);
    }
    
    /**
     * Find user by ID - alias for getUserById for consistency
     */
    public Optional<User> findById(String id) {
        return getUserById(id);
    }
    
    /**
     * Save user - wrapper for repository save
     * Đảm bảo luôn có password (DB column NOT NULL), kể cả với tài khoản OAuth2
     */
    public User save(User user) {
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            // Tạo mật khẩu ngẫu nhiên, đã mã hóa, để thỏa constraint DB.
            // Người dùng OAuth2 sẽ đăng nhập bằng Google/Facebook nên không dùng mật khẩu này.
            String randomPassword = UUID.randomUUID().toString();
            user.setPassword(passwordEncoder.encode(randomPassword));
        }
        return userRepository.save(user);
    }
    
    /**
     * Check password for debugging (remove in production)
     */
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * Change user role
     */
    public boolean changeUserRole(String userId, UserRole newRole, String reason, String changedBy) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserRole oldRole = user.getRole();
            
            // Check if it's a downgrade
            boolean isDowngrade = isRoleDowngrade(oldRole, newRole);
            
            // Update role
            user.setRole(newRole);
            userRepository.save(user);
            
            logger.info("User {} role changed from {} to {} by admin {}", 
                user.getUsername(), oldRole, newRole, changedBy);
            
            // Send WebSocket notification to user about role change
            try {
                sendRoleChangeNotification(user.getId(), user.getUsername(), oldRole.name(), newRole.name(), reason, changedBy, isDowngrade);
            } catch (Exception e) {
                logger.error("Failed to send role change notification for user {}: {}", userId, e.getMessage());
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Send role change notification via WebSocket
     */
    private void sendRoleChangeNotification(String userId, String username, String oldRole, String newRole, 
                                           String reason, String changedBy, boolean isDowngrade) {
        try {
            String notificationUrl = notificationServiceBaseUrl + "/api/notifications/role-change";
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("username", username);
            notification.put("oldRole", oldRole);
            notification.put("newRole", newRole);
            notification.put("reason", reason);
            notification.put("changedBy", changedBy);
            notification.put("isDowngrade", isDowngrade);
            notification.put("type", "ROLE_CHANGE");
            
            restTemplate.postForEntity(notificationUrl, notification, Map.class);
            
            logger.info("✅ Sent role change notification for user: userId={}, {} -> {}", 
                userId, oldRole, newRole);
        } catch (Exception e) {
            logger.error("❌ Failed to send role change notification: {}", e.getMessage());
        }
    }
    
    /**
     * Change role (legacy method)
     */
    public boolean changeRole(String userId, String role) {
        try {
            UserRole userRole = UserRole.valueOf(role);
            return changeUserRole(userId, userRole, "Role changed via API", "system");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if it's a role downgrade
     */
    private boolean isRoleDowngrade(UserRole oldRole, UserRole newRole) {
        int oldPriority = getRolePriority(oldRole);
        int newPriority = getRolePriority(newRole);
        
        // If from USER to any other role, it's not a downgrade
        if (oldRole == UserRole.USER && newRole != UserRole.USER) {
            return false;
        }
        
        return newPriority < oldPriority;
    }
    
    /**
     * Get role priority
     */
    private int getRolePriority(UserRole role) {
        switch (role) {
            case ADMIN:
                return 6;
            case MODERATOR:
                return 5;
            case PRODUCT_MANAGER:
                return 4;
            case USER_MANAGER:
                return 3;
            case SUPPORT:
                return 2;
            case USER:
                return 1;
            default:
                return 0;
        }
    }
    
    /**
     * Reset admin password (for testing purposes)
     */
    public boolean resetAdminPassword(String newPassword) {
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            admin.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(admin);
            return true;
        }
        return false;
    }
    
    /**
     * Get user by identifier (username or email)
     */
    public Optional<User> getByIdentifier(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier);
    }
    
    /**
     * Check if user is locked
     */
    public boolean isUserLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }
    
    /**
     * Unlock user
     */
    public void unlockUser(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLockedUntil(null);
            user.setLoginAttempts(0);
            userRepository.save(user);
        }
    }

    /**
     * Send phone OTP
     */
    public boolean sendPhoneOtp(String phone) {
        try {
            // Generate 6-digit OTP
            String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
         
            logger.info("OTP for phone {}: {}", phone, otp);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send phone OTP", e);
            return false;
        }
    }

    /**
     * Verify phone OTP
     */
    public Map<String, Object> verifyPhoneOtp(String phone, String otp, String username, 
                                             String password, String firstName, String lastName, String email) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // For now, just validate OTP format (in real implementation, check database)
            if (otp.length() != 6) {
                result.put("success", false);
                result.put("message", "OTP không đúng");
                return result;
            }
            
            // Check if user already exists with this phone
            Optional<User> existingUser = userRepository.findByPhoneNumber(phone);
            User user;
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // Create new user
                if (username == null || password == null) {
                    result.put("success", false);
                    result.put("message", "Thiếu username/password để tạo tài khoản mới");
                    return result;
                }
                
                user = createUser(username, email != null ? email : (username + "@phone.local"), 
                                password, firstName, lastName, phone);
                user.setIsEmailVerified(true); // Treat phone verification as sufficient
                user.setIsActive(true);
                userRepository.save(user);
            }
            
            result.put("success", true);
            result.put("message", "Xác minh số điện thoại thành công");
            result.put("user", user);
            
        } catch (Exception e) {
            logger.error("Failed to verify phone OTP", e);
            result.put("success", false);
            result.put("message", "Xác minh OTP thất bại: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Create OAuth2 user
     */
    public User createOAuth2User(String email, String username, String password) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsEmailVerified(true);
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Get users by role
     */
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrueAndIsDeletedFalse(role);
    }
}