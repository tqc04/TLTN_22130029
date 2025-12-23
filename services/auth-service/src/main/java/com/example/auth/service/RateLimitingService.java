package com.example.auth.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using Bucket4j with Redis backend
 * Provides distributed rate limiting across multiple service instances
 */
@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    @Value("${rate.limit.login.requests:5}")
    private int loginRequestsLimit;

    @Value("${rate.limit.login.duration:60}")
    private int loginDurationSeconds;

    private final RedisTemplate<String, String> redisTemplate;
    
    // Fallback in-memory cache if Redis is unavailable
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();
    private boolean useRedis = false;

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            // Test Redis connection
            redisTemplate.opsForValue().set("rate-limit-test", "test", Duration.ofSeconds(1));
            redisTemplate.delete("rate-limit-test");
            useRedis = true;
            logger.info("Rate limiting service initialized with Redis. Login limit: {} requests per {} seconds", 
                loginRequestsLimit, loginDurationSeconds);
        } catch (Exception e) {
            logger.warn("Failed to connect to Redis for rate limiting. Falling back to in-memory cache. Error: {}", e.getMessage());
            useRedis = false;
        }
    }

    /**
     * Check if request is allowed for the given key (typically IP address)
     * @param key Identifier for rate limiting (e.g., IP address)
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key) {
        try {
            Bucket bucket = getBucket(key);
            boolean allowed = bucket.tryConsume(1);
            
            // Update Redis counter if using Redis
            if (useRedis && allowed) {
                String redisKey = "rate-limit:login:" + key;
                Long count = redisTemplate.opsForValue().increment(redisKey);
                if (count != null && count == 1) {
                    // Set expiration on first increment
                    redisTemplate.expire(redisKey, Duration.ofSeconds(loginDurationSeconds));
                }
            }
            
            return allowed;
        } catch (Exception e) {
            logger.error("Error checking rate limit for key: {}. Error: {}", key, e.getMessage());
            // On error, allow the request (fail open) to avoid blocking legitimate users
            return true;
        }
    }

    /**
     * Get remaining tokens for the given key
     * @param key Identifier for rate limiting
     * @return Number of remaining tokens
     */
    public long getAvailableTokens(String key) {
        try {
            Bucket bucket = getBucket(key);
            return bucket.getAvailableTokens();
        } catch (Exception e) {
            logger.error("Error getting available tokens for key: {}. Error: {}", key, e.getMessage());
            return loginRequestsLimit;
        }
    }

    /**
     * Get or create bucket for the given key
     */
    private Bucket getBucket(String key) {
        if (useRedis) {
            // Check Redis counter first
            String redisKey = "rate-limit:login:" + key;
            String countStr = redisTemplate.opsForValue().get(redisKey);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            // If already at limit, return a bucket that will deny
            if (currentCount >= loginRequestsLimit) {
                // Create a bucket with 0 tokens
                Bandwidth limit = Bandwidth.builder()
                    .capacity(0)
                    .refillIntervally(0, Duration.ofSeconds(loginDurationSeconds))
                    .build();
                return Bucket.builder().addLimit(limit).build();
            }
        }
        
        // Use in-memory bucket (works for single instance or as fallback)
        return localBuckets.computeIfAbsent(key, k -> createLocalBucket());
    }

    /**
     * Create bucket configuration with rate limit settings
     */
    private Bucket createLocalBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(loginRequestsLimit)
            .refillIntervally(loginRequestsLimit, Duration.ofSeconds(loginDurationSeconds))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Reset rate limit for a specific key (useful for testing or manual unlock)
     */
    public void resetLimit(String key) {
        try {
            if (useRedis) {
                String redisKey = "rate-limit:login:" + key;
                redisTemplate.delete(redisKey);
            }
            localBuckets.remove(key);
            logger.info("Rate limit reset for key: {}", key);
        } catch (Exception e) {
            logger.error("Error resetting rate limit for key: {}. Error: {}", key, e.getMessage());
        }
    }
}
