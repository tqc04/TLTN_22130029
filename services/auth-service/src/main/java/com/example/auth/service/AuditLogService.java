package com.example.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final String KEY = "auth:audit";
    private static final int MAX_LOGS = 5000; // cap to avoid unbounded growth

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void record(String action, String userId, Map<String, Object> details) {
        try {
            Map<String, Object> entry = new HashMap<>();
            entry.put("ts", Instant.now().toString());
            entry.put("action", action);
            if (userId != null) {
                entry.put("userId", userId);
            }
            if (details != null) {
                entry.put("details", details);
            }
            String json = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForList().leftPush(KEY, json);
            redisTemplate.opsForList().trim(KEY, 0, MAX_LOGS - 1);
            // Also log to stdout/ELK pipeline
            log.info("AUDIT action={} userId={} details={}", action, userId, details);
        } catch (Exception e) {
            log.warn("Failed to record audit log: {}", e.getMessage());
        }
    }
}


