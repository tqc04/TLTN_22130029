package com.example.order.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    private final Map<String, Integer> productIdToStock = new ConcurrentHashMap<>();

    public int getStock(String productId) {
        return productIdToStock.getOrDefault(productId, 0);
    }

    public int updateStock(String productId, int stockQuantity) {
        productIdToStock.put(productId, stockQuantity);
        return stockQuantity;
    }
}


