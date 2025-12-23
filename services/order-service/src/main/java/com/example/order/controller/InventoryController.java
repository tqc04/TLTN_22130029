package com.example.order.controller;

import com.example.order.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable String productId) {
        int stock = inventoryService.getStock(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateStock(@PathVariable String productId,
                                                           @RequestBody Map<String, Object> body) {
        int qty = Integer.parseInt(body.getOrDefault("stockQuantity", 0).toString());
        int stock = inventoryService.updateStock(productId, qty);
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }
}


