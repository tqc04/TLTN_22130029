package com.example.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String RT_KEY_PREFIX = "auth:rt:";
    private static final String USER_SET_PREFIX = "auth:rt:user:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RefreshTokenService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${security.jwt.refreshTtlMillis:259200000}") long refreshTtlMillis // default 3 days
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMillis(refreshTtlMillis > 0 ? refreshTtlMillis : 259200000L);
    }

    public String issue(String userId) {
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String key = RT_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, userId, ttl);
        // track per-user to revoke all
        redisTemplate.opsForSet().add(userSetKey(userId), token);
        redisTemplate.expire(userSetKey(userId), ttl);
        return token;
    }

    public String validateAndRotate(String oldToken) {
        String key = RT_KEY_PREFIX + oldToken;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return null;
        }
        // revoke old
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(userSetKey(userId), oldToken);
        // issue new
        return issue(userId);
    }

    public void revoke(String token) {
        String key = RT_KEY_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (userId != null) {
            redisTemplate.opsForSet().remove(userSetKey(userId), token);
        }
    }

    public void revokeAllForUser(String userId) {
        try {
            var members = redisTemplate.opsForSet().members(userSetKey(userId));
            if (members != null) {
                for (String token : members) {
                    redisTemplate.delete(RT_KEY_PREFIX + token);
                }
            }
            redisTemplate.delete(userSetKey(userId));
        } catch (Exception e) {
            log.warn("Failed to revoke all refresh tokens for user {}: {}", userId, e.getMessage());
        }
    }

    private String userSetKey(String userId) {
        return USER_SET_PREFIX + userId;
    }
}


