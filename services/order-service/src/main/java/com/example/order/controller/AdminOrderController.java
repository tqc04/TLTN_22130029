package com.example.order.controller;

import com.example.order.entity.OrderStatus;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {

        String statusStr = body.get("status");
        String trackingNumber = body.get("trackingNumber");

        if (statusStr == null || statusStr.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Status is required");
            return ResponseEntity.badRequest().body(response);
        }

        return orderService.findById(orderId)
            .map(order -> {
                try {
                    OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
                    OrderStatus oldStatus = order.getStatus();

                    order.setStatus(newStatus);

                    if (trackingNumber != null && !trackingNumber.isEmpty()) {
                        order.setTrackingNumber(trackingNumber);
                    }

                    if (newStatus == OrderStatus.DELIVERED) {
                        order.setDeliveredDate(java.time.LocalDateTime.now());
                    }

                    orderService.updateStatus(order, oldStatus, newStatus);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Order status updated successfully");
                    response.put("orderId", orderId);
                    response.put("newStatus", newStatus.name());
                    return ResponseEntity.ok(response);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", "Invalid status: " + statusStr);
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .orElseGet(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Order not found");
                return ResponseEntity.status(404).body(response);
            });
    }
}

