package com.example.user.repository;

import com.example.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    Optional<User> findByEmailVerificationToken(String emailVerificationToken);
    List<User> findByIsActiveTrueAndIsDeletedFalse();
    List<User> findByIsActiveFalseAndIsDeletedFalse();
    Page<User> findByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    List<User> findByRoleAndIsActiveTrueAndIsDeletedFalse(com.example.user.entity.UserRole role);
}


