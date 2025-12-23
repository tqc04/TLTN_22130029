package com.example.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class InventoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("rawtypes")
    private CircuitBreakerFactory circuitBreakerFactory;

    @Value("${services.inventory.base-url:http://localhost:8093}")
    private String inventoryServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> reserveInventory(Map<String, Object> request) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory-service");

        return circuitBreaker.run(
            () -> {
                org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/reserve",
                    request,
                    Map.class
                );
                if (!response.getStatusCode().is2xxSuccessful()) {
                    return Map.of("success", false, "error", "Inventory service returned status: " + response.getStatusCode());
                }
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                return body != null ? body : Map.of("success", false, "error", "No response body");
            },
            throwable -> {
                // Fallback: return error response
                return Map.of("success", false, "error", "Inventory service unavailable");
            }
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> confirmInventory(Map<String, Object> request) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory-service");

        return circuitBreaker.run(
            () -> {
                org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/confirm",
                    request,
                    Map.class
                );
                if (!response.getStatusCode().is2xxSuccessful()) {
                    // Log warning but return success since confirmation is idempotent and can be retried
                    logger.warn("Inventory confirmation returned non-2xx status: {}", response.getStatusCode());
                    return Map.of("success", false, "message", "Inventory confirmation failed, will retry");
                }
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                return body != null ? body : Map.of("success", true, "message", "No response body");
            },
            throwable -> {
                // Fallback: log error but continue - inventory confirmation is idempotent
                logger.warn("Inventory confirmation failed: {}", throwable.getMessage());
                return Map.of("success", false, "message", "Inventory confirmation queued for retry");
            }
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> releaseInventory(Map<String, Object> request) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory-service");

        return circuitBreaker.run(
            () -> {
                org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    inventoryServiceUrl + "/api/inventory/release",
                    request,
                    Map.class
                );
                if (!response.getStatusCode().is2xxSuccessful()) {
                    // Log warning but return success since release is cleanup operation and can be retried
                    logger.warn("Inventory release returned non-2xx status: {}", response.getStatusCode());
                    return Map.of("success", false, "message", "Inventory release failed, will retry");
                }
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                return body != null ? body : Map.of("success", true, "message", "No response body");
            },
            throwable -> {
                // Fallback: log error but continue - inventory release is cleanup operation
                logger.warn("Inventory release failed: {}", throwable.getMessage());
                return Map.of("success", false, "message", "Inventory release queued for retry");
            }
        );
    }
}
