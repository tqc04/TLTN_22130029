package com.example.order.controller;

import com.example.order.dto.OrderDTO;
import com.example.order.dto.OrderItemDTO;
import com.example.order.entity.Order;
import com.example.order.service.InventoryServiceClient;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;


    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OrderDTO>> list(@RequestParam(required = false) String status, Pageable pageable) {
        Page<Order> orders = (status != null && !status.isBlank())
                ? orderService.findByStatus(com.example.order.entity.OrderStatus.valueOf(status), pageable)
                : orderService.findAll(pageable);
        return ResponseEntity.ok(orders.map(OrderDTO::from));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDTO> get(@PathVariable Long id) {
        return orderService.findById(id)
                .map(o -> ResponseEntity.ok(OrderDTO.from(o)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OrderDTO>> myOrders(@RequestParam String userId, Pageable pageable) {
        Page<Order> orders = orderService.findByUserId(userId, pageable);
        return ResponseEntity.ok(orders.map(OrderDTO::from));
    }

    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderDTO> getByNumber(@PathVariable String orderNumber) {
        return orderService.findByOrderNumber(orderNumber)
                .map(o -> ResponseEntity.ok(OrderDTO.from(o)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Get purchased products for user (for review validation)
     * Allows internal service calls via X-Internal-Request header
     */
    @GetMapping("/user/{userId}/purchased-products")
    public ResponseEntity<Map<String, Object>> getPurchasedProducts(
            @PathVariable String userId,
            @RequestHeader(value = "X-Internal-Request", required = false) String internalRequest) {
        try {
            // Allow internal service calls without authentication
            if (!"true".equals(internalRequest)) {
                // For external calls, would need authentication
                // For now, we allow it for simplicity
                logger.warn("External call to purchased-products endpoint");
            }
            
            List<String> purchasedProductIds = orderService.getPurchasedProductIds(userId);
            return ResponseEntity.ok(Map.of("products", purchasedProductIds));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("")
    @Transactional
    public ResponseEntity<OrderDTO> create(@RequestBody OrderDTO payload, @RequestHeader(value = "X-User-Id", required = false) String userIdFromHeader) {
        // Validate that order has items
        if (payload.getOrderItems() == null || payload.getOrderItems().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        
        // Generate order number first for proper tracking
        if (payload.getOrderNumber() == null || payload.getOrderNumber().isBlank()) {
            payload.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        }
        
        try {
            // Build Order entity with items
            // NOTE: Inventory reservation is handled inside OrderService.create() to avoid duplicate reservation
            com.example.order.entity.Order order = payload.toEntity();

            // Debug logging
            logger.debug("Order payload paymentMethod: {}", payload.getPaymentMethod());
            logger.debug("Order entity paymentMethod after toEntity(): {}", order.getPaymentMethod());

            // Set userId from multiple sources (in order of preference)
            logger.debug("Initial userId from payload: {}", order.getUserId());
            logger.debug("userIdFromHeader: {}", userIdFromHeader);

            // Always ensure userId is set - this is critical for order creation
            if (order.getUserId() == null) {
                // First try from header (most common in microservices with Gateway)
                if (userIdFromHeader != null) {
                    order.setUserId(userIdFromHeader);
                    logger.debug("Set userId from header: {}", userIdFromHeader);
                } else {
                    // Try from SecurityContext - get authenticated user ID
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication != null && !authentication.getClass().getSimpleName().equals("AnonymousAuthenticationToken")) {
                        logger.debug("Valid authentication found: {}", authentication.getClass());
                        try {
                            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
                                // JWT Authentication - extract from claims
                                org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth =
                                    (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication;
                                org.springframework.security.oauth2.jwt.Jwt jwt = jwtAuth.getToken();

                                // Try different claim names for user ID
                                String userId = null;
                                if (jwt.hasClaim("userId")) {
                                    Object userIdClaim = jwt.getClaim("userId");
                                    userId = userIdClaim != null ? userIdClaim.toString() : null;
                                } else if (jwt.hasClaim("id")) {
                                    Object idClaim = jwt.getClaim("id");
                                    userId = idClaim != null ? idClaim.toString() : null;
                                } else if (jwt.hasClaim("sub")) {
                                    userId = jwt.getSubject();
                                }

                                if (userId != null && !userId.isEmpty()) {
                                    order.setUserId(userId);
                                    logger.debug("Set userId from JWT: {}", userId);
                                } else {
                                    throw new RuntimeException("User ID not found in JWT token claims");
                                }
                            } else if (authentication.getPrincipal() instanceof java.util.Map) {
                                // Fallback for other authentication types
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> principal = (java.util.Map<String, Object>) authentication.getPrincipal();
                                Object userIdObj = principal.get("id");
                                if (userIdObj != null) {
                                    order.setUserId(userIdObj.toString());
                                    logger.debug("Set userId from SecurityContext Map: {}", userIdObj);
                                } else {
                                    throw new RuntimeException("User ID not found in authentication token");
                                }
                            } else {
                                throw new RuntimeException("Invalid authentication principal format");
                            }
                        } catch (Exception e) {
                            logger.error("Error processing authentication: {}", e.getMessage());
                            throw new RuntimeException("Authentication required to create order: " + e.getMessage());
                        }
                    } else {
                        throw new RuntimeException("Authentication required to create order");
                    }
                }
            }

            logger.debug("Final userId: {}", order.getUserId());
            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
            
            for (OrderItemDTO item : payload.getOrderItems()) {
                com.example.order.entity.OrderItem oi = new com.example.order.entity.OrderItem();
                oi.setProductId(item.getProductId());
                oi.setProductName(item.getProductName());
                oi.setProductSku(item.getProductSku() != null ? item.getProductSku() : "");
                oi.setProductImage(item.getProductImage());
                oi.setQuantity(item.getQuantity());
                oi.setUnitPrice(item.getUnitPrice());
                if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    java.math.BigDecimal itemTotal = item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()));
                    oi.setTotalPrice(itemTotal);
                    subtotal = subtotal.add(itemTotal);
                }
                order.addOrderItem(oi);
            }
            
            // Calculate order totals SERVER-SIDE (SECURITY: Never trust client calculations!)
            order.setSubtotal(subtotal);
            
            // Calculate tax (10%)
            java.math.BigDecimal tax = subtotal.multiply(new java.math.BigDecimal("0.1"));
            order.setTaxAmount(tax);
            
            // Get shipping fee from payload or calculate (free if subtotal > 500000 VND)
            java.math.BigDecimal shippingFee;
            if (payload.getShippingFee() != null) {
                shippingFee = payload.getShippingFee();
            } else {
                // Free shipping if subtotal > 500,000 VND (consistent with cart logic)
                shippingFee = subtotal.compareTo(new java.math.BigDecimal("500000")) > 0 ? 
                    java.math.BigDecimal.ZERO : new java.math.BigDecimal("30000");
            }
            order.setShippingFee(shippingFee);
            order.setShippingAmount(shippingFee);
            
            // IMPORTANT: Do NOT trust discount from client.
            // Base total is subtotal + tax + shipping. Voucher discount will be validated
            // and applied server-side in OrderService.processVoucher using voucher-service.
            order.setDiscountAmount(java.math.BigDecimal.ZERO);
            java.math.BigDecimal baseTotalAmount = subtotal.add(tax).add(shippingFee);
            order.setTotalAmount(baseTotalAmount);

            // Set initial order status and payment status
            // IMPORTANT: For VNPay payments, order is saved with PROCESSING status
            // If payment fails, it will be cancelled and removed from DB via the cancellation endpoint
            String pm = order.getPaymentMethod() != null ? order.getPaymentMethod().toUpperCase() : "";
            if (pm.contains("COD")) {
                order.setStatus(com.example.order.entity.OrderStatus.PENDING);
                order.setPaymentStatus(com.example.order.entity.PaymentStatus.PENDING);
            } else {
                // For VNPay payment - set to PROCESSING, will be updated after payment confirmation
                order.setStatus(com.example.order.entity.OrderStatus.PROCESSING);
                order.setPaymentStatus(com.example.order.entity.PaymentStatus.PROCESSING);
            }
            
            Order created = orderService.create(order);
            return ResponseEntity.ok(OrderDTO.from(created));
        } catch (Exception ex) {
            // OrderService.create() handles inventory rollback internally if order creation fails
            logger.error("Failed to create order: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<java.util.Map<String, Object>> confirm(@PathVariable Long orderId) {
        return orderService.findById(orderId)
            .map(order -> {
                // Validate order can be confirmed
                if (order.getStatus() == com.example.order.entity.OrderStatus.CONFIRMED) {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", true);
                    response.put("message", "Order already confirmed");
                    return ResponseEntity.ok(response);
                }
                
                if (order.getStatus() == com.example.order.entity.OrderStatus.CANCELLED) {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", false);
                    response.put("error", "Cannot confirm cancelled order");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Confirm inventory for each order item
                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    try {
                        java.util.Map<String, Object> req = new java.util.HashMap<>();
                        req.put("productId", item.getProductId());
                        req.put("quantity", item.getQuantity());
                        req.put("orderId", order.getOrderNumber());
                        inventoryServiceClient.confirmInventory(req);
                    } catch (Exception e) {
                        // Log error but continue - inventory confirmation is idempotent
                        System.err.println("Failed to confirm inventory for product " + item.getProductId() + ": " + e.getMessage());
                    }
                }
                
                order.setStatus(com.example.order.entity.OrderStatus.CONFIRMED);
                // updatedAt will be set automatically by @LastModifiedDate
                orderService.update(order); // Use update, not create
                
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", true);
                response.put("message", "Order confirmed successfully");
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/by-number/{orderNumber}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> cancelByNumber(@PathVariable String orderNumber,
                                                                        @RequestBody java.util.Map<String, String> body) {
        String reason = body.getOrDefault("reason", "User requested cancellation");

        return orderService.findByOrderNumber(orderNumber)
            .map(order -> {
                // Validate order can be cancelled
                if (order.getStatus() == com.example.order.entity.OrderStatus.CANCELLED) {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", true);
                    response.put("message", "Order already cancelled");
                    return ResponseEntity.ok(response);
                }

                if (order.getStatus() == com.example.order.entity.OrderStatus.DELIVERED ||
                    order.getStatus() == com.example.order.entity.OrderStatus.COMPLETED) {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", false);
                    response.put("error", "Cannot cancel delivered/completed order");
                    return ResponseEntity.badRequest().body(response);
                }

                // Check if this is a payment cancellation (during checkout) or order cancellation (after placement)
                boolean isPaymentCancellation = order.getStatus() == com.example.order.entity.OrderStatus.PROCESSING 
                    && order.getPaymentStatus() == com.example.order.entity.PaymentStatus.PROCESSING
                    && reason.contains("Payment failed");

                // Release inventory for each item
                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    try {
                        java.util.Map<String, Object> req = new java.util.HashMap<>();
                        req.put("productId", item.getProductId());
                        req.put("quantity", item.getQuantity());
                        req.put("orderId", order.getOrderNumber());
                        inventoryServiceClient.releaseInventory(req);
                    } catch (Exception e) {
                        System.err.println("Failed to release inventory for product " + item.getProductId() + ": " + e.getMessage());
                    }
                }

                java.util.Map<String, Object> response = new java.util.HashMap<>();
                
                if (isPaymentCancellation) {
                    // Payment cancellation during checkout: DELETE from database (don't save cancellation)
                    try {
                        orderService.deleteOrder(order.getId());
                        response.put("success", true);
                        response.put("message", "Order deleted - payment was cancelled");
                        response.put("deleted", true);
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        System.err.println("Failed to delete order during payment cancellation: " + e.getMessage());
                        // Fallback to cancellation if deletion fails
                    }
                }

                // Order cancellation after placement: UPDATE status to CANCELLED (save in database)
                boolean ok = orderService.cancel(order.getId(), reason);

                if (ok) {
                    response.put("success", true);
                    response.put("message", "Order cancelled successfully");
                    response.put("deleted", false);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("error", "Failed to cancel order");
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .orElseGet(() -> {
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("error", "Order not found");
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Cancel order after it has been placed (this saves the cancellation to database)
     * This is different from payment cancellation which deletes the order entirely
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> cancel(@PathVariable Long orderId,
                                                                @RequestBody java.util.Map<String, String> body) {
        String reason = body.getOrDefault("reason", "User requested cancellation");
        
        return orderService.findById(orderId)
            .map(order -> {
                // Validate order can be cancelled
                if (order.getStatus() == com.example.order.entity.OrderStatus.CANCELLED) {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", true);
                    response.put("message", "Order already cancelled");
                    return ResponseEntity.ok(response);
                }
                
                if (order.getStatus() == com.example.order.entity.OrderStatus.DELIVERED ||
                    order.getStatus() == com.example.order.entity.OrderStatus.COMPLETED) {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", false);
                    response.put("error", "Cannot cancel delivered/completed order");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Release inventory for each item to restore stock
                logger.info("Releasing inventory for {} items in order {}", order.getOrderItems().size(), order.getOrderNumber());
                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    try {
                        java.util.Map<String, Object> req = new java.util.HashMap<>();
                        req.put("productId", item.getProductId());
                        req.put("quantity", item.getQuantity());
                        req.put("orderId", order.getOrderNumber());
                        inventoryServiceClient.releaseInventory(req);
                        logger.info("Released {} units of product {}", item.getQuantity(), item.getProductId());
                    } catch (Exception e) {
                        System.err.println("Failed to release inventory for product " + item.getProductId() + ": " + e.getMessage());
                    }
                }
                
                // Cancel the order (this saves the cancellation record to database)
                boolean ok = orderService.cancel(orderId, reason);
                
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                if (ok) {
                    response.put("success", true);
                    response.put("message", "Order cancelled successfully and inventory restored");
                    response.put("deleted", false); // Order is saved as CANCELLED, not deleted
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("error", "Failed to cancel order");
                    return ResponseEntity.badRequest().body(response);
                }
            })
            .orElseGet(() -> {
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("error", "Order not found");
                return ResponseEntity.notFound().build();
            });
    }

    @GetMapping("/flagged")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OrderDTO>> flagged(Pageable pageable) {
        Page<Order> orders = orderService.findFlagged(pageable);
        return ResponseEntity.ok(orders.map(OrderDTO::from));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> stats() {
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("PENDING", orderService.countByStatus(com.example.order.entity.OrderStatus.PENDING));
        out.put("CONFIRMED", orderService.countByStatus(com.example.order.entity.OrderStatus.CONFIRMED));
        out.put("PROCESSING", orderService.countByStatus(com.example.order.entity.OrderStatus.PROCESSING));
        out.put("SHIPPED", orderService.countByStatus(com.example.order.entity.OrderStatus.SHIPPED));
        out.put("DELIVERED", orderService.countByStatus(com.example.order.entity.OrderStatus.DELIVERED));
        out.put("CANCELLED", orderService.countByStatus(com.example.order.entity.OrderStatus.CANCELLED));
        out.put("REFUNDED", orderService.countByStatus(com.example.order.entity.OrderStatus.REFUNDED));
        out.put("COMPLETED", orderService.countByStatus(com.example.order.entity.OrderStatus.COMPLETED));
        out.put("FLAGGED", orderService.countFlagged());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/top-products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<Map<String, Object>> topProducts = orderService.findTopProducts(safeLimit);

        Map<String, Object> out = Map.of(
                "products", topProducts,
                "count", topProducts.size()
        );
        return ResponseEntity.ok(out);
    }
    
    @GetMapping("/category-distribution")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> categoryDistribution() {
        List<Map<String, Object>> distribution = orderService.getCategoryDistribution();
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * Update order status (for admin)
     */
    /**
     * Confirm order after successful payment (for payment gateway callbacks)
     */
    @PostMapping("/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Object>> confirmPayment(
            @RequestBody java.util.Map<String, String> body) {
        try {
            String orderNumber = body.get("orderNumber");
            String transactionId = body.get("transactionId");

            if (orderNumber == null || orderNumber.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "error", "orderNumber is required"));
            }

            Order confirmedOrder = orderService.confirmPaymentAndOrder(orderNumber, transactionId);

            if (confirmedOrder != null) {
                return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Order confirmed successfully",
                    "orderId", confirmedOrder.getId(),
                    "orderNumber", confirmedOrder.getOrderNumber()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<java.util.Map<String, Object>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String statusStr = body.get("status");
            String trackingNumber = body.get("trackingNumber");
            
            if (statusStr == null || statusStr.isEmpty()) {
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", false);
                response.put("error", "Status is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            return orderService.findById(orderId)
                .map(order -> {
                    try {
                        com.example.order.entity.OrderStatus oldStatus = order.getStatus();
                        com.example.order.entity.OrderStatus newStatus = 
                            com.example.order.entity.OrderStatus.valueOf(statusStr.toUpperCase());
                        
                        // Only update if status actually changed
                        if (oldStatus.equals(newStatus)) {
                            java.util.Map<String, Object> response = new java.util.HashMap<>();
                            response.put("success", true);
                            response.put("message", "Order status unchanged");
                            response.put("orderId", orderId);
                            response.put("status", newStatus.name());
                            return ResponseEntity.ok(response);
                        }
                        
                        order.setStatus(newStatus);
                        
                        // Set tracking number if provided
                        if (trackingNumber != null && !trackingNumber.isEmpty()) {
                            order.setTrackingNumber(trackingNumber);
                        }
                        
                        // Set delivered date if status is DELIVERED
                        if (newStatus == com.example.order.entity.OrderStatus.DELIVERED) {
                            order.setDeliveredDate(java.time.LocalDateTime.now());
                        }
                        
                        orderService.updateStatus(order, oldStatus, newStatus);
                        
                        java.util.Map<String, Object> response = new java.util.HashMap<>();
                        response.put("success", true);
                        response.put("message", "Order status updated successfully");
                        response.put("orderId", orderId);
                        response.put("newStatus", newStatus.name());
                        return ResponseEntity.ok(response);
                    } catch (IllegalArgumentException e) {
                        java.util.Map<String, Object> response = new java.util.HashMap<>();
                        response.put("success", false);
                        response.put("error", "Invalid status: " + statusStr);
                        return ResponseEntity.badRequest().body(response);
                    }
                })
                .orElseGet(() -> {
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", false);
                    response.put("error", "Order not found");
                    return ResponseEntity.notFound().build();
                });
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
