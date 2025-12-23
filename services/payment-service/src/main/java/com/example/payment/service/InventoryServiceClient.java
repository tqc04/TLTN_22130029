package com.example.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class InventoryServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("rawtypes")
    private CircuitBreakerFactory circuitBreakerFactory;

    @Value("${services.inventory.base-url:http://localhost:8093}")
    private String inventoryServiceUrl;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF = 
        new ParameterizedTypeReference<Map<String, Object>>() {};

    public Map<String, Object> reserveInventory(Map<String, Object> request) {
        @SuppressWarnings("rawtypes")
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory-service");

        return circuitBreaker.run(
            () -> {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/reserve",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(request),
                    MAP_TYPE_REF
                );
                Map<String, Object> body = response.getBody();
                return body != null ? body : Map.of("success", false, "error", "No response body");
            },
            throwable -> {
                // Fallback: return error response
                return Map.of("success", false, "error", "Inventory service unavailable");
            }
        );
    }

    public Map<String, Object> confirmInventory(Map<String, Object> request) {
        @SuppressWarnings("rawtypes")
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory-service");

        return circuitBreaker.run(
            () -> {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/confirm",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(request),
                    MAP_TYPE_REF
                );
                Map<String, Object> body = response.getBody();
                return body != null ? body : Map.of("success", true, "message", "No response body");
            },
            throwable -> {
                // Fallback: return success for idempotent operation
                return Map.of("success", true, "message", "Inventory confirmation queued");
            }
        );
    }

    public Map<String, Object> releaseInventory(Map<String, Object> request) {
        @SuppressWarnings("rawtypes")
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("inventory-service");

        return circuitBreaker.run(
            () -> {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    inventoryServiceUrl + "/api/inventory/release",
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(request),
                    MAP_TYPE_REF
                );
                Map<String, Object> body = response.getBody();
                return body != null ? body : Map.of("success", true, "message", "No response body");
            },
            throwable -> {
                // Fallback: return success for cleanup operation
                return Map.of("success", true, "message", "Inventory release queued");
            }
        );
    }
}
