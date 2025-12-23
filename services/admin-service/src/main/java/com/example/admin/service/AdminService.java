package com.example.admin.service;

import com.example.admin.entity.AdminUser;
import com.example.admin.entity.AdminRole;
import com.example.admin.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@Transactional
public class AdminService {
    
    @Autowired
    private AdminUserRepository adminUserRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Create admin user
     */
    public AdminUser createAdminUser(String username, String email, String password, 
                                   String firstName, String lastName, AdminRole role) {
        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setEmail(email);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setFirstName(firstName);
        adminUser.setLastName(lastName);
        adminUser.setRole(role);
        adminUser.setIsActive(true);
        adminUser.setPasswordChangedAt(LocalDateTime.now());
        
        return adminUserRepository.save(adminUser);
    }

    /**
     * Authenticate admin user
     */
    public Optional<AdminUser> authenticateAdmin(String usernameOrEmail, String password) {
        Optional<AdminUser> adminOpt = adminUserRepository.findByUsernameOrEmail(usernameOrEmail);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            if (passwordEncoder.matches(password, admin.getPassword()) && 
                Boolean.TRUE.equals(admin.getIsActive()) && 
                !isUserLocked(admin)) {
                
                // Update last login
                admin.setLastLoginAt(LocalDateTime.now());
                admin.setLoginAttempts(0);
                adminUserRepository.save(admin);
                
                return Optional.of(admin);
            } else {
                // Increment failed login attempts
                admin.setLoginAttempts(admin.getLoginAttempts() + 1);
                if (admin.getLoginAttempts() >= 5) {
                    admin.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                }
                adminUserRepository.save(admin);
            }
        }
        return Optional.empty();
    }

    /**
     * Check if user is locked
     */
    public boolean isUserLocked(AdminUser admin) {
        return admin.getLockedUntil() != null && admin.getLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Unlock admin user
     */
    public void unlockAdminUser(Long adminId) {
        Optional<AdminUser> adminOpt = adminUserRepository.findById(adminId);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            admin.setLockedUntil(null);
            admin.setLoginAttempts(0);
            adminUserRepository.save(admin);
        }
    }

    /**
     * Change admin password
     */
    public boolean changePassword(Long adminId, String currentPassword, String newPassword) {
        Optional<AdminUser> adminOpt = adminUserRepository.findById(adminId);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            if (passwordEncoder.matches(currentPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(newPassword));
                admin.setPasswordChangedAt(LocalDateTime.now());
                adminUserRepository.save(admin);
                return true;
            }
        }
        return false;
    }

    /**
     * Update admin profile
     */
    public AdminUser updateAdminProfile(Long adminId, Map<String, Object> profileData) {
        Optional<AdminUser> adminOpt = adminUserRepository.findById(adminId);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            
            if (profileData.containsKey("firstName")) {
                admin.setFirstName((String) profileData.get("firstName"));
            }
            if (profileData.containsKey("lastName")) {
                admin.setLastName((String) profileData.get("lastName"));
            }
            if (profileData.containsKey("email")) {
                admin.setEmail((String) profileData.get("email"));
            }
            
            return adminUserRepository.save(admin);
        }
        return null;
    }

    /**
     * Change admin role
     */
    public boolean changeAdminRole(Long adminId, AdminRole newRole, String reason, String changedBy) {
        Optional<AdminUser> adminOpt = adminUserRepository.findById(adminId);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            admin.setRole(newRole);
            adminUserRepository.save(admin);
            return true;
        }
        return false;
    }

    /**
     * Toggle admin status
     */
    public boolean toggleAdminStatus(Long adminId, Boolean isActive) {
        Optional<AdminUser> adminOpt = adminUserRepository.findById(adminId);
        if (adminOpt.isPresent()) {
            AdminUser admin = adminOpt.get();
            admin.setIsActive(isActive);
            adminUserRepository.save(admin);
            return true;
        }
        return false;
    }

    /**
     * Get admin by ID
     */
    public Optional<AdminUser> findById(Long id) {
        return adminUserRepository.findById(id);
    }

    /**
     * Get admin by username or email
     */
    public Optional<AdminUser> findByUsernameOrEmail(String usernameOrEmail) {
        return adminUserRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    /**
     * Get all admins with pagination
     */
    public Page<AdminUser> findAll(Pageable pageable) {
        return adminUserRepository.findAll(pageable);
    }

    /**
     * Get admins by role
     */
    public List<AdminUser> findByRole(AdminRole role) {
        return adminUserRepository.findByRole(role);
    }

    /**
     * Get active admins
     */
    public List<AdminUser> findActiveAdmins() {
        return adminUserRepository.findByIsActive(true);
    }

    /**
     * Get locked admins
     */
    public List<AdminUser> findLockedAdmins() {
        return adminUserRepository.findLockedUsers(LocalDateTime.now());
    }

    /**
     * Get recently active admins
     */
    public List<AdminUser> findRecentlyActiveAdmins() {
        return adminUserRepository.findRecentlyActiveUsers(LocalDateTime.now().minusDays(7));
    }

    /**
     * Get admin statistics
     */
    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalAdmins", adminUserRepository.count());
        stats.put("activeAdmins", adminUserRepository.countByRoleAndIsActive(AdminRole.ADMIN));
        stats.put("superAdmins", adminUserRepository.countByRoleAndIsActive(AdminRole.SUPER_ADMIN));
        stats.put("moderators", adminUserRepository.countByRoleAndIsActive(AdminRole.MODERATOR));
        stats.put("support", adminUserRepository.countByRoleAndIsActive(AdminRole.SUPPORT));
        stats.put("analysts", adminUserRepository.countByRoleAndIsActive(AdminRole.ANALYST));
        
        return stats;
    }

    /**
     * Delete admin user
     */
    public void deleteAdminUser(Long adminId) {
        adminUserRepository.deleteById(adminId);
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return adminUserRepository.findByUsername(username).isPresent();
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return adminUserRepository.findByEmail(email).isPresent();
    }
}
