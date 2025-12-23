package com.example.admin.repository;

import com.example.admin.entity.AdminUser;
import com.example.admin.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    
    Optional<AdminUser> findByUsername(String username);
    
    Optional<AdminUser> findByEmail(String email);
    
    List<AdminUser> findByRole(AdminRole role);
    
    List<AdminUser> findByIsActive(Boolean isActive);
    
    List<AdminUser> findByRoleAndIsActive(AdminRole role, Boolean isActive);
    
    @Query("SELECT a FROM AdminUser a WHERE a.username = :usernameOrEmail OR a.email = :usernameOrEmail")
    Optional<AdminUser> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    
    @Query("SELECT a FROM AdminUser a WHERE a.lockedUntil IS NOT NULL AND a.lockedUntil > :now")
    List<AdminUser> findLockedUsers(@Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM AdminUser a WHERE a.lastLoginAt >= :startDate ORDER BY a.lastLoginAt DESC")
    List<AdminUser> findRecentlyActiveUsers(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(a) FROM AdminUser a WHERE a.role = :role AND a.isActive = true")
    Long countByRoleAndIsActive(@Param("role") AdminRole role);
    
    @Query("SELECT a FROM AdminUser a WHERE a.twoFactorEnabled = true")
    List<AdminUser> findUsersWithTwoFactor();
}
