package com.example.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Order Management Controller
 * Handles order approval, rejection, and status updates
 */
@RestController
@RequestMapping("/api/admin/orders")
public class OrderManagementController {

    private static final Logger logger = LoggerFactory.getLogger(OrderManagementController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.order.base-url:http://localhost:8084}")
    private String orderServiceUrl;

    /**
     * Get all orders with pagination
     */
    @GetMapping("")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        try {
            String url = orderServiceUrl + "/api/orders?page=" + page + "&size=" + size;
            if (status != null && !status.isEmpty()) {
                url += "&status=" + status;
            }
            
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching orders: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            String url = orderServiceUrl + "/api/orders/" + orderId;
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching order {}: {}", orderId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Approve order (Confirm and start processing)
     */
    @PostMapping("/{orderId}/approve")
    public ResponseEntity<Map<String, Object>> approveOrder(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            // Confirm the order
            String confirmUrl = orderServiceUrl + "/api/orders/" + orderId + "/confirm";
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> confirmResponse = restTemplate.exchange(confirmUrl, HttpMethod.POST, new HttpEntity<>(headers), Map.class);
            
            if (confirmResponse.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Order approved and confirmed");
                result.put("orderId", orderId);
                
                logger.info("Admin approved order: {}", orderId);
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to approve order"
                ));
            }
        } catch (Exception e) {
            logger.error("Error approving order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Reject/Cancel order
     */
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<Map<String, Object>> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String reason = body.getOrDefault("reason", "Admin rejected order");
            
            // Cancel the order
            String cancelUrl = orderServiceUrl + "/api/orders/" + orderId + "/cancel";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("reason", reason);
            
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> cancelResponse = restTemplate.exchange(cancelUrl, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
            
            if (cancelResponse.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Order rejected and cancelled");
                result.put("orderId", orderId);
                result.put("reason", reason);
                
                logger.info("Admin rejected order {}: {}", orderId, reason);
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to reject order"
                ));
            }
        } catch (Exception e) {
            logger.error("Error rejecting order {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Update order status to PROCESSING
     */
    @PostMapping("/{orderId}/process")
    public ResponseEntity<Map<String, Object>> startProcessing(@PathVariable Long orderId, HttpServletRequest request) {
        try {
            String url = orderServiceUrl + "/api/orders/" + orderId + "/status";
            Map<String, String> requestBody = Map.of("status", "PROCESSING");
            
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Admin started processing order: {}", orderId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Order marked as processing",
                    "orderId", orderId
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to update order status"
                ));
            }
        } catch (Exception e) {
            logger.error("Error updating order {} to processing: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Update order status to SHIPPED
     */
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<Map<String, Object>> markAsShipped(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String trackingNumber = body.get("trackingNumber");
            
            String url = orderServiceUrl + "/api/orders/" + orderId + "/status";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("status", "SHIPPED");
            if (trackingNumber != null) {
                requestBody.put("trackingNumber", trackingNumber);
            }
            
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Admin marked order {} as shipped with tracking: {}", orderId, trackingNumber);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Order marked as shipped",
                    "orderId", orderId,
                    "trackingNumber", trackingNumber != null ? trackingNumber : "N/A"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to update order status"
                ));
            }
        } catch (Exception e) {
            logger.error("Error marking order {} as shipped: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Update order status (generic endpoint for any status)
     * Supports both PUT and POST methods for compatibility
     */
    @PutMapping("/{orderId}/status")
    @PostMapping("/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String status = body.get("status");
            String reason = body.get("reason");
            String trackingNumber = body.get("trackingNumber");
            
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Status is required"
                ));
            }
            
            String url = orderServiceUrl + "/api/orders/" + orderId + "/status";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("status", status);
            if (trackingNumber != null && !trackingNumber.isEmpty()) {
                requestBody.put("trackingNumber", trackingNumber);
            }
            if (reason != null && !reason.isEmpty()) {
                requestBody.put("reason", reason);
            }
            
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Admin updated order {} status to: {}", orderId, status);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Order status updated successfully");
                result.put("orderId", orderId);
                result.put("newStatus", status);
                if (response.getBody() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
                    result.putAll(responseBody);
                }
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(Map.of(
                    "success", false,
                    "error", "Failed to update order status"
                ));
            }
        } catch (Exception e) {
            logger.error("Error updating order {} status: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get pending orders (awaiting approval)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            String url = orderServiceUrl + "/api/orders?page=" + page + "&size=" + size + "&status=PENDING";
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching pending orders: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get flagged orders (high risk)
     */
    @GetMapping("/flagged")
    public ResponseEntity<?> getFlaggedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            String url = orderServiceUrl + "/api/orders/flagged?page=" + page + "&size=" + size;
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching flagged orders: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get order statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats(HttpServletRequest request) {
        try {
            String url = orderServiceUrl + "/api/orders/stats";
            HttpHeaders headers = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching order stats: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

