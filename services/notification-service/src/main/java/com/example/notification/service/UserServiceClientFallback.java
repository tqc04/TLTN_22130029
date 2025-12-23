package com.example.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public Map<String, Object> getUserById(String userId) {
        logger.warn("Fallback triggered for getUserById, user-service unavailable. userId={}", userId);
        return Map.of(
                "success", false,
                "fallback", true,
                "message", "user-service unavailable, using fallback",
                "userId", userId
        );
    }

    @Override
    public List<Map<String, Object>> getAdminUsers() {
        logger.warn("Fallback triggered for getAdminUsers, user-service unavailable");
        return List.of();
    }
}
