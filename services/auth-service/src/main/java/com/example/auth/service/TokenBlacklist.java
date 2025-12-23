package com.example.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlacklist {
    private final Map<String, Long> inMemory = new ConcurrentHashMap<>();
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "auth:blacklist:";

    public TokenBlacklist(org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revoke(String token, long expiryEpochMs) {
        inMemory.put(token, expiryEpochMs);
        if (redisTemplate != null) {
            long ttl = Math.max(1, (expiryEpochMs - System.currentTimeMillis()) / 1000);
            redisTemplate.opsForValue().set(KEY_PREFIX + token, "1", java.time.Duration.ofSeconds(ttl));
        }
    }

    public boolean isRevoked(String token) {
        // check redis first
        if (redisTemplate != null) {
            String val = redisTemplate.opsForValue().get(KEY_PREFIX + token);
            if (val != null) return true;
        }
        Long exp = inMemory.get(token);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            inMemory.remove(token);
            return false;
        }
        return true;
    }
    
    /**
     * Cleanup expired tokens every hour to prevent memory leak
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        inMemory.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}


