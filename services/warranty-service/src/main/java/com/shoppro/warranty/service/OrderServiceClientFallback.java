package com.shoppro.warranty.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceClientFallback.class);

    @Override
    public Map<String, Object> getOrderByNumber(String orderNumber) {
        // Provide a non-null safe fallback response and log the incident
        logger.error("Fallback triggered for getOrderByNumber, order-service unavailable. orderNumber={}", orderNumber);
        return Map.of(
                "success", false,
                "fallback", true,
                "message", "order-service unavailable, using fallback",
                "orderNumber", orderNumber
        );
    }
}
