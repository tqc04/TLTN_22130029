package com.shoppro.warranty.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppro.warranty.dto.WarrantyRequestDTO;
import com.shoppro.warranty.entity.WarrantyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String WARRANTY_REQUEST_KEY_PREFIX = "warranty:request:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Cache warranty request
     */
    public void cacheWarrantyRequest(WarrantyRequest request) {
        try {
            String key = WARRANTY_REQUEST_KEY_PREFIX + request.getId();
            WarrantyRequestDTO dto = WarrantyRequestDTO.from(request);
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to cache warranty request: " + e.getMessage());
        }
    }

    /**
     * Get cached warranty request
     */
    public Optional<WarrantyRequestDTO> getCachedWarrantyRequest(Long id) {
        try {
            String key = WARRANTY_REQUEST_KEY_PREFIX + id;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                String json = (String) cached;
                WarrantyRequestDTO dto = objectMapper.readValue(json, WarrantyRequestDTO.class);
                return Optional.of(dto);
            }
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to get cached warranty request: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Delete cached warranty request
     */
    public void deleteCachedWarrantyRequest(Long id) {
        try {
            String key = WARRANTY_REQUEST_KEY_PREFIX + id;
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Failed to delete cached warranty request: " + e.getMessage());
        }
    }

    /**
     * Cache warranty request by user ID
     */
    public void cacheWarrantyRequestsByUser(String userId, String requestsJson) {
        try {
            String key = "warranty:user:" + userId;
            redisTemplate.opsForValue().set(key, requestsJson, CACHE_TTL);
        } catch (Exception e) {
            System.err.println("Failed to cache warranty requests by user: " + e.getMessage());
        }
    }

    /**
     * Get cached order details
     */
    public Optional<Map<String, Object>> getCachedOrderDetails(String orderNumber) {
        try {
            String key = "order:" + orderNumber;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                String json = (String) cached;
                Map<String, Object> orderDetails = objectMapper.readValue(json, Map.class);
                return Optional.of(orderDetails);
            }
        } catch (Exception e) {
            System.err.println("Failed to get cached order details: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Cache order details
     */
    public void cacheOrderDetails(String orderNumber, Map<String, Object> orderDetails) {
        try {
            String key = "order:" + orderNumber;
            String json = objectMapper.writeValueAsString(orderDetails);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(24)); // Cache for 24 hours
        } catch (Exception e) {
            System.err.println("Failed to cache order details: " + e.getMessage());
        }
    }

    /**
     * Get cached user warranty requests
     */
    public Optional<List<WarrantyRequestDTO>> getCachedUserWarrantyRequests(String userId, int page, int size) {
        try {
            String key = "warranty:user:" + userId + ":page:" + page + ":size:" + size;
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                String json = (String) cached;
                List<WarrantyRequestDTO> requests = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WarrantyRequestDTO.class));
                return Optional.of(requests);
            }
        } catch (Exception e) {
            System.err.println("Failed to get cached user warranty requests: " + e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Cache user warranty requests
     */
    public void cacheUserWarrantyRequests(String userId, List<WarrantyRequestDTO> requests, int page, int size) {
        try {
            String key = "warranty:user:" + userId + ":page:" + page + ":size:" + size;
            String json = objectMapper.writeValueAsString(requests);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception e) {
            System.err.println("Failed to cache user warranty requests: " + e.getMessage());
        }
    }
}
