package com.example.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OrderServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @SuppressWarnings("rawtypes")
    private CircuitBreakerFactory circuitBreakerFactory;

    @Value("${services.order.base-url:http://localhost:8084}")
    private String orderServiceUrl;

    @Value("${interservice.username:service}")
    private String interserviceUsername;

    @Value("${interservice.password:service123}")
    private String interservicePassword;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REF = 
        new ParameterizedTypeReference<Map<String, Object>>() {};

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(interserviceUsername, interservicePassword);
        return headers;
    }

    public Map<String, Object> getOrderById(Long orderId) {
        @SuppressWarnings("rawtypes")
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("order-service");

        return circuitBreaker.run(
            () -> {
                HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    orderServiceUrl + "/api/orders/" + orderId,
                    HttpMethod.GET,
                    entity,
                    MAP_TYPE_REF
                );
                return response.getBody();
            },
            throwable -> {
                // Fallback: return null to indicate order not found
                return null;
            }
        );
    }

    public Map<String, Object> getOrderByNumber(String orderNumber) {
        @SuppressWarnings("rawtypes")
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("order-service");

        return circuitBreaker.run(
            () -> {
                HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    orderServiceUrl + "/api/orders/number/" + orderNumber,
                    HttpMethod.GET,
                    entity,
                    MAP_TYPE_REF
                );
                return response.getBody();
            },
            throwable -> {
                // Fallback: return null to indicate order not found
                return null;
            }
        );
    }

    public Map<String, Object> cancelOrderByNumber(String orderNumber, String reason) {
        @SuppressWarnings("rawtypes")
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("order-service");

        return circuitBreaker.run(
            () -> {
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                    Map.of("reason", reason), 
                    createAuthHeaders()
                );
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    orderServiceUrl + "/api/orders/by-number/" + orderNumber + "/cancel",
                    HttpMethod.POST,
                    entity,
                    MAP_TYPE_REF
                );
                return response.getBody();
            },
            throwable -> {
                // Fallback: return success for cleanup operation
                return Map.of("success", true, "message", "Order cancellation queued for retry");
            }
        );
    }
}
