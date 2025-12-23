package com.example.user.service;

import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Request password reset - generates token but doesn't send email
     * Email sending should be handled by the controller via notification service
     */
    public String requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return null;
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        return token;
    }

    /**
     * Reset password using token
     */
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByPasswordResetToken(token);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        if (user.getPasswordResetTokenExpiresAt() == null || user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return false;
        }
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
        return true;
    }

    /**
     * Find user by reset token
     */
    public Optional<User> findByResetToken(String token) {
        return userRepository.findByPasswordResetToken(token);
    }
}


