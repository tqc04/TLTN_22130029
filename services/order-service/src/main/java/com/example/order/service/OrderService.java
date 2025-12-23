package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@EnableCaching
@Transactional(rollbackFor = Exception.class)
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private com.example.order.repository.OrderItemRepository orderItemRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.notification.base-url:http://localhost:8086}")
    private String notificationServiceUrl;

    @Value("${services.inventory.base-url:http://localhost:8093}")
    private String inventoryServiceUrl;

    @Value("${services.payment.base-url:http://localhost:8085}")
    private String paymentServiceUrl;

    @Value("${services.product.base-url:http://localhost:8088}")
    private String productServiceUrl;
	
	@Value("${interservice.username:service}")
	private String interserviceUsername;

	@Value("${interservice.password:service123}")
	private String interservicePassword;

    @Value("${services.voucher.base-url:http://localhost:8092}")
    private String voucherServiceUrl;

    @Value("${services.cart.base-url:http://localhost:8084}")
    private String cartServiceUrl;
    
    @Value("${services.category.base-url:http://localhost:8089}")
    private String categoryServiceUrl;

    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAllWithOrderItems(pageable);
    }

    @Cacheable(value = "orders", key = "#id")
    public Optional<Order> findById(Long id) {
        return orderRepository.findByIdWithOrderItems(id);
    }

    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatusWithOrderItems(status, pageable);
    }

    public Page<Order> findFlagged(Pageable pageable) {
        return orderRepository.findFlaggedWithOrderItems(pageable);
    }

    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public long countFlagged() { return orderRepository.countByIsFlaggedForReviewTrue(); }

    public Page<Order> findByUserId(String userId, Pageable pageable) {
        return orderRepository.findByUserIdWithOrderItems(userId, pageable);
    }

    @Cacheable(value = "orders", key = "#orderNumber")
    public java.util.Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumberWithOrderItems(orderNumber);
    }
    
    /**
     * Get purchased product IDs for user (for review validation)
     */
    public List<String> getPurchasedProductIds(String userId) {
        List<Order> completedOrders = orderRepository.findByUserIdAndStatusInWithOrderItems(
            userId,
            List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
        );

        return completedOrders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .map(item -> item.getProductId())
            .distinct()
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Create order with Saga pattern for distributed transaction
     */
    @CacheEvict(value = "orders", allEntries = true)
    public Order create(Order order) {
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Debug logging for payment method
        logger.info("Creating order with paymentMethod: {}", order.getPaymentMethod());

        try {
            // Check if order number already exists (avoid duplicate orders)
            if (order.getOrderNumber() != null && !order.getOrderNumber().isBlank()) {
                Optional<Order> existingOrder = orderRepository.findByOrderNumber(order.getOrderNumber());
                if (existingOrder.isPresent()) {
                    logger.warn("Order with number {} already exists, returning existing order", order.getOrderNumber());
                    return existingOrder.get();
                }
            }

            // Step 0: Handle voucher if provided
            if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
                processVoucher(order);
            }

            // Step 1: Reserve inventory
            reserveInventory(order);

            // Step 2: Process payment synchronously (NOT async)
            processPayment(order);

            // Step 3: Save order only after successful payment
            Order savedOrder = orderRepository.save(order);

            // Step 4: Confirm inventory reservation (only after successful payment)
            // For COD payments, inventory will be confirmed when admin/staff confirms the order via /{orderId}/confirm endpoint
            // For VNPay, inventory will be confirmed after payment callback via confirmPaymentAndOrder()
            if (savedOrder.getPaymentStatus() == com.example.order.entity.PaymentStatus.COMPLETED) {
                confirmInventory(savedOrder);
            }

            // Step 5: Send notification (async)
            sendOrderNotification(savedOrder);

            return savedOrder;

        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage());
            // Rollback inventory if needed
            rollbackInventory(order);
            throw new RuntimeException("Order creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create HTTP headers with Basic Auth for inter-service communication
     */
    private org.springframework.http.HttpHeaders createServiceHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBasicAuth(interserviceUsername, interservicePassword);
        return headers;
    }

    /**
     * Reserve inventory from Inventory Service
     */
    private void reserveInventory(Order order) {
        try {
            org.springframework.http.HttpHeaders headers = createServiceHeaders();
            
            // Use batch reserve if there are multiple items
            if (order.getOrderItems().size() > 1) {
                String url = inventoryServiceUrl + "/api/inventory/reserve-batch";

                List<Map<String, Object>> items = order.getOrderItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productId", item.getProductId());
                        itemMap.put("quantity", item.getQuantity());
                        return itemMap;
                    })
                    .collect(java.util.stream.Collectors.toList());

                Map<String, Object> request = new HashMap<>();
                request.put("orderId", order.getOrderNumber());
                request.put("items", items);

                org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                    new org.springframework.http.HttpEntity<>(request, headers);
                
                @SuppressWarnings({"rawtypes"})
                org.springframework.http.ResponseEntity response = restTemplate.postForEntity(url, entity, Map.class);
                Object responseBody = response.getBody();
                if (!response.getStatusCode().is2xxSuccessful() || !(responseBody instanceof Map) ||
                    !Boolean.TRUE.equals(((Map<?, ?>) responseBody).get("success"))) {
                    String errorMsg = responseBody instanceof Map ? 
                        ((Map<?, ?>) responseBody).get("error").toString() : "Unknown error";
                    logger.error("Inventory reservation failed for order {}: {}", order.getOrderNumber(), errorMsg);
                    throw new RuntimeException("Failed to reserve inventory for order " + order.getOrderNumber() + ": " + errorMsg);
                }
            } else {
                // Use single item reserve for single item orders
                String url = inventoryServiceUrl + "/api/inventory/reserve";

                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    Map<String, Object> request = new HashMap<>();
                    request.put("productId", item.getProductId());
                    request.put("quantity", item.getQuantity());
                    request.put("orderId", order.getOrderNumber());

                    org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                        new org.springframework.http.HttpEntity<>(request, headers);

                    @SuppressWarnings({"rawtypes"})
                    org.springframework.http.ResponseEntity resp = restTemplate.postForEntity(url, entity, Map.class);
                    Object respBody = resp.getBody();
                    if (!resp.getStatusCode().is2xxSuccessful() || !(respBody instanceof Map) ||
                        !Boolean.TRUE.equals(((Map<?, ?>) respBody).get("success"))) {
                        String errorMsg = respBody instanceof Map ? 
                            ((Map<?, ?>) respBody).get("error").toString() : "Unknown error";
                        logger.error("Inventory reservation failed for product {}: {}", item.getProductId(), errorMsg);
                        throw new RuntimeException("Failed to reserve inventory for product " + item.getProductId() + ": " + errorMsg);
                    }
                }
            }

            logger.info("Inventory reserved for order: {}", order.getOrderNumber());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("Failed to reserve inventory for order {}: {} : {}",
                order.getOrderNumber(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Inventory reservation failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Failed to reserve inventory for order {}: {}",
                order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Inventory reservation failed", e);
        }
    }
    
    /**
     * Process payment synchronously (handle payment logic directly in order service)
     */
    private void processPayment(Order order) {
        try {
            logger.info("Processing payment with method: {} for order: {}", order.getPaymentMethod(), order.getOrderNumber());

            // Handle different payment methods
            String paymentMethod = order.getPaymentMethod();
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                paymentMethod = "VNPAY";
                logger.warn("No payment method specified for order: {}, defaulting to VNPAY", order.getOrderNumber());
            }

            switch (paymentMethod.toUpperCase()) {
                case "COD":
                case "CASH_ON_DELIVERY":
                    // COD orders remain pending until staff confirmation
                    order.setPaymentStatus(com.example.order.entity.PaymentStatus.PENDING);
                    order.setStatus(com.example.order.entity.OrderStatus.PENDING);
                    logger.info("COD order created in pending state, awaiting staff confirmation: {}", order.getOrderNumber());
                    break;

                case "VNPAY":
                    // VNPay payments need to redirect to gateway
                    order.setPaymentStatus(com.example.order.entity.PaymentStatus.PROCESSING);
                    order.setStatus(com.example.order.entity.OrderStatus.PROCESSING);
                    logger.info("VNPay payment initiated for order: {}, redirecting to gateway", order.getOrderNumber());
                    break;

                default:
                    logger.warn("Unknown payment method: {}, defaulting to VNPAY", paymentMethod);
                    order.setPaymentStatus(com.example.order.entity.PaymentStatus.PROCESSING);
                    order.setStatus(com.example.order.entity.OrderStatus.PROCESSING);
                    break;
            }

        } catch (Exception e) {
            logger.error("Failed to process payment for order {}: {} : {}",
                order.getOrderNumber(), e.getClass().getSimpleName(), e.getMessage());
            // Don't save order to database if payment fails
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Rollback inventory reservation
     * Made public so it can be called from scheduled tasks and other services
     */
    public void rollbackInventoryForOrder(Order order) {
        rollbackInventory(order);
    }
    
    /**
     * Rollback inventory reservation (private implementation)
     */
    private void rollbackInventory(Order order) {
        try {
            org.springframework.http.HttpHeaders headers = createServiceHeaders();
            
            // Use batch release if there are multiple items
            if (order.getOrderItems().size() > 1) {
                String url = inventoryServiceUrl + "/api/inventory/release";

                List<Map<String, Object>> items = order.getOrderItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productId", item.getProductId());
                        itemMap.put("quantity", item.getQuantity());
                        return itemMap;
                    })
                    .collect(java.util.stream.Collectors.toList());

                Map<String, Object> request = new HashMap<>();
                request.put("orderId", order.getOrderNumber());
                request.put("items", items);

                org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                    new org.springframework.http.HttpEntity<>(request, headers);
                restTemplate.postForEntity(url, entity, Map.class);
            } else {
                // Use single item release for single item orders
                String url = inventoryServiceUrl + "/api/inventory/release";

                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    Map<String, Object> singleRequest = new HashMap<>();
                    singleRequest.put("orderId", order.getOrderNumber());
                    singleRequest.put("productId", item.getProductId());
                    singleRequest.put("quantity", item.getQuantity());

                    org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                        new org.springframework.http.HttpEntity<>(singleRequest, headers);
                    restTemplate.postForEntity(url, entity, Map.class);
                }
            }

            logger.info("Inventory rollback completed for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to rollback inventory for order {}: {}",
                order.getOrderNumber(), e.getMessage());
        }
    }

    /**
     * Confirm inventory reservation (when order is successfully confirmed)
     */
    private void confirmInventory(Order order) {
        try {
            org.springframework.http.HttpHeaders headers = createServiceHeaders();
            
            // Use batch confirm if there are multiple items
            if (order.getOrderItems().size() > 1) {
                String url = inventoryServiceUrl + "/api/inventory/confirm-batch";

                List<Map<String, Object>> items = order.getOrderItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productId", item.getProductId());
                        itemMap.put("quantity", item.getQuantity());
                        return itemMap;
                    })
                    .collect(java.util.stream.Collectors.toList());

                Map<String, Object> request = new HashMap<>();
                request.put("orderId", order.getOrderNumber());
                request.put("items", items);

                org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                    new org.springframework.http.HttpEntity<>(request, headers);
                restTemplate.postForEntity(url, entity, Map.class);
            } else {
                // Use single item confirm for single item orders
                String url = inventoryServiceUrl + "/api/inventory/confirm";

                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    Map<String, Object> request = new HashMap<>();
                    request.put("orderId", order.getOrderNumber());
                    request.put("productId", item.getProductId());
                    request.put("quantity", item.getQuantity());

                    org.springframework.http.HttpEntity<Map<String, Object>> entity = 
                        new org.springframework.http.HttpEntity<>(request, headers);
                    restTemplate.postForEntity(url, entity, Map.class);
                }
            }

            logger.info("Inventory confirmation completed for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to confirm inventory for order {}: {}",
                order.getOrderNumber(), e.getMessage());
            // Note: This is not critical enough to fail the order creation
            // In production, you might want to implement retry logic or manual intervention
        }
    }

    /**
     * Send order notification to Notification Service
     */
    private void sendOrderNotification(Order order) {
        try {
            String url = notificationServiceUrl + "/api/notifications/order";
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", order.getUserId());
            notification.put("orderNumber", order.getOrderNumber());
            notification.put("status", order.getStatus().name());
            notification.put("totalAmount", order.getTotalAmount());
            
            // Use async execution to avoid blocking order creation
            // Set a short timeout (2 seconds) so it doesn't block order creation
            try {
                restTemplate.postForEntity(url, notification, Map.class);
                logger.info("Order notification sent for order: {}", order.getOrderNumber());
            } catch (org.springframework.web.client.ResourceAccessException e) {
                // Timeout or connection error - log but don't fail
                logger.warn("Notification service timeout/connection error for order {}: {}", 
                    order.getOrderNumber(), e.getMessage());
            }
        } catch (Exception e) {
            // Log error but don't fail order creation
            logger.error("Failed to send order notification for order {}: {}", 
                order.getOrderNumber(), e.getMessage());
        }
    }
    
    /**
     * Send order status change notification
     */
    private void sendOrderStatusChangeNotification(Order order, com.example.order.entity.OrderStatus oldStatus, 
                                                   com.example.order.entity.OrderStatus newStatus) {
        try {
            String url = notificationServiceUrl + "/api/notifications/order/status-change";
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", order.getUserId());
            notification.put("orderId", order.getId());
            notification.put("orderNumber", order.getOrderNumber());
            notification.put("oldStatus", oldStatus.name());
            notification.put("newStatus", newStatus.name());
            notification.put("totalAmount", order.getTotalAmount());
            if (order.getTrackingNumber() != null) {
                notification.put("trackingNumber", order.getTrackingNumber());
            }
            
            restTemplate.postForEntity(url, notification, Map.class);
            logger.info("Order status change notification sent for order: {} ({} -> {})", 
                order.getOrderNumber(), oldStatus, newStatus);
        } catch (Exception e) {
            logger.error("Failed to send order status change notification for order {}: {}", 
                order.getOrderNumber(), e.getMessage());
        }
    }
    
    @CacheEvict(value = "orders", key = "#order.id")
    public Order update(Order order) {
        return orderRepository.save(order);
    }
    
    /**
     * Update order status and send notification
     */
    @CacheEvict(value = "orders", key = "#order.id")
    public Order updateStatus(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        Order updatedOrder = orderRepository.save(order);
        
        // Send notification if status changed
        if (!oldStatus.equals(newStatus)) {
            sendOrderStatusChangeNotification(updatedOrder, oldStatus, newStatus);
        }
        
        return updatedOrder;
    }

    /**
     * Confirm order after successful payment (for VNPay callbacks)
     */
    @CacheEvict(value = "orders", allEntries = true)
    public Order confirmPaymentAndOrder(String orderNumber, String transactionId) {
        return orderRepository.findByOrderNumber(orderNumber).map(order -> {
            // Update payment status
            order.setPaymentStatus(com.example.order.entity.PaymentStatus.COMPLETED);
            order.setPaymentReference(transactionId);
            order.setPaymentDate(java.time.LocalDateTime.now());

            // Update order status
            order.setStatus(com.example.order.entity.OrderStatus.CONFIRMED);

            // Confirm inventory reservation
            confirmInventory(order);

            return orderRepository.save(order);
        }).orElse(null);
    }

    /**
     * Process voucher during order creation
     */
    private void processVoucher(Order order) {
        try {
            logger.info("Processing voucher {} for order {}", order.getVoucherCode(), order.getOrderNumber());

            // ---- Step 1: Validate voucher and calculate discount server-side ----
            java.math.BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal tax = order.getTaxAmount() != null ? order.getTaxAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal originalAmount = subtotal.add(tax).add(shipping);

            // Build validation request payload
            java.util.List<Map<String, Object>> items = order.getOrderItems().stream()
                .map(item -> {
                    // Enrich with category/brand from product-service
                    Map<String, Object> meta = fetchProductMeta(item.getProductId());
                    Long categoryId = meta.get("categoryId") instanceof Number ? ((Number) meta.get("categoryId")).longValue() : null;
                    Long brandId = meta.get("brandId") instanceof Number ? ((Number) meta.get("brandId")).longValue() : null;

                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", item.getProductId());
                    m.put("productName", item.getProductName());
                    m.put("categoryId", categoryId);
                    m.put("brandId", brandId);
                    m.put("price", item.getUnitPrice());
                    m.put("quantity", item.getQuantity());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());

            Map<String, Object> validationRequest = new HashMap<>();
            validationRequest.put("voucherCode", order.getVoucherCode());
            validationRequest.put("userId", order.getUserId());
            validationRequest.put("orderAmount", originalAmount);
            validationRequest.put("items", items);

            @SuppressWarnings({"rawtypes"})
            org.springframework.http.ResponseEntity validateResp = restTemplate.postForEntity(
                voucherServiceUrl + "/api/vouchers/validate",
                validationRequest,
                Map.class);

            Object validateBodyObj = validateResp.getBody();
            if (!validateResp.getStatusCode().is2xxSuccessful() || !(validateBodyObj instanceof Map)) {
                throw new RuntimeException("Voucher validation failed with status: " + validateResp.getStatusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> validateBody = (Map<String, Object>) validateBodyObj;
            Object validFlag = validateBody.get("valid");
            if (!(validFlag instanceof Boolean) || !((Boolean) validFlag)) {
                String msg = validateBody.get("message") != null ? validateBody.get("message").toString()
                        : "Voucher validation failed";
                throw new RuntimeException(msg);
            }

            // Extract discount and final amount from validation response
            java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;
            java.math.BigDecimal finalAmount = originalAmount;

            Object discountObj = validateBody.get("discountAmount");
            if (discountObj != null) {
                discountAmount = new java.math.BigDecimal(discountObj.toString());
            }

            Object finalAmountObj = validateBody.get("finalAmount");
            if (finalAmountObj != null) {
                finalAmount = new java.math.BigDecimal(finalAmountObj.toString());
            } else {
                finalAmount = originalAmount.subtract(discountAmount);
            }

            // Update order with trusted values from voucher-service
            order.setDiscountAmount(discountAmount);
            order.setTotalAmount(finalAmount);

            // Also capture voucherId from response if available
            Object voucherIdObj = validateBody.get("voucherId");
            if (voucherIdObj != null) {
                try {
                    order.setVoucherId(Long.valueOf(voucherIdObj.toString()));
                } catch (NumberFormatException ignored) {
                    logger.warn("Unable to parse voucherId from validation response for order {}", order.getOrderNumber());
                }
            }

            // ---- Step 2: Record voucher usage ----
            // Note: order.getId() may be null here since order hasn't been saved yet
            // Use orderNumber as the primary identifier for voucher usage tracking
            Map<String, Object> usageRequest = new HashMap<>();
            usageRequest.put("voucherId", order.getVoucherId());
            usageRequest.put("voucherCode", order.getVoucherCode());
            usageRequest.put("userId", order.getUserId());
            // orderId is optional - voucher service can use orderNumber instead
            if (order.getId() != null) {
                usageRequest.put("orderId", order.getId());
            }
            usageRequest.put("orderNumber", order.getOrderNumber());
            usageRequest.put("originalAmount", originalAmount);
            usageRequest.put("discountAmount", discountAmount);
            usageRequest.put("finalAmount", finalAmount);

            @SuppressWarnings({"rawtypes"})
            org.springframework.http.ResponseEntity usageResp = restTemplate.postForEntity(
                voucherServiceUrl + "/api/vouchers/usage",
                usageRequest,
                Map.class);

            if (!usageResp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to record voucher usage, status: " + usageResp.getStatusCode());
            }

            logger.info("Successfully validated and recorded voucher usage for order {}", order.getOrderNumber());

        } catch (Exception e) {
            logger.error("Error processing voucher for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            // Continue with order creation even if voucher processing fails
            // In production, you might want to fail the order or handle this differently
        }
    }

    /**
     * Fetch product metadata (categoryId, brandId) from product-service for voucher validation
     */
    private Map<String, Object> fetchProductMeta(String productId) {
        try {
            String url = productServiceUrl + "/api/products/" + productId;
            
            // Add authentication headers for inter-service communication
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            String credentials = interserviceUsername + ":" + interservicePassword;
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            headers.set("Authorization", "Basic " + encodedCredentials);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef =
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
            org.springframework.http.ResponseEntity<Map<String, Object>> resp =
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, typeRef);
            
            if (resp.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = resp.getBody();
                if (body != null && body.containsKey("categoryId") && body.containsKey("brandId")) {
                    return body;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch product meta for {}: {}", productId, e.getMessage());
        }
        // Return empty map if fetch fails - voucher validation will proceed without category/brand restrictions
        return Map.of();
    }

    public boolean cancel(Long orderId, String reason) {
        return orderRepository.findById(orderId).map(o -> {
			// Only allow cancel when order is not already final
			if (o.getStatus() == OrderStatus.CANCELLED
					|| o.getStatus() == OrderStatus.DELIVERED
					|| o.getStatus() == OrderStatus.COMPLETED
					|| o.getStatus() == OrderStatus.FAILED) {
				logger.warn("Attempted to cancel order {} with final status {}", o.getOrderNumber(), o.getStatus());
				return false;
			}

			// If VNPay and payment already completed -> trigger refund via payment-service
			try {
				if ("VNPAY".equalsIgnoreCase(o.getPaymentMethod())
						&& o.getPaymentStatus() == com.example.order.entity.PaymentStatus.COMPLETED) {

					String url = paymentServiceUrl + "/api/payments/internal/refund-by-order/" + o.getOrderNumber();

					org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
					headers.setBasicAuth(interserviceUsername, interservicePassword);

					Map<String, Object> body = new HashMap<>();
					body.put("reason", reason != null ? reason : "Order cancelled by customer");

					org.springframework.http.HttpEntity<Map<String, Object>> request =
							new org.springframework.http.HttpEntity<>(body, headers);

					@SuppressWarnings({"rawtypes"})
					org.springframework.http.ResponseEntity<Map> resp =
							restTemplate.postForEntity(url, request, Map.class);

					Object responseBody = resp.getBody();
					boolean refundSuccess = responseBody instanceof Map &&
							Boolean.TRUE.equals(((Map<?, ?>) responseBody).get("success"));

					if (refundSuccess) {
						o.setPaymentStatus(com.example.order.entity.PaymentStatus.REFUNDED);
						logger.info("Refund successful for VNPay order {}", o.getOrderNumber());
					} else {
						logger.error("Refund failed for VNPay order {}: {}", o.getOrderNumber(), responseBody);
					}
				}

				// For COD or unpaid orders, simply mark payment as cancelled
				if (!"VNPAY".equalsIgnoreCase(o.getPaymentMethod())
						&& o.getPaymentStatus() == com.example.order.entity.PaymentStatus.PENDING) {
					o.setPaymentStatus(com.example.order.entity.PaymentStatus.CANCELLED);
				}
			} catch (Exception e) {
				logger.error("Error while processing refund for order {}: {}", o.getOrderNumber(), e.getMessage());
			}

			// Finally, mark order as cancelled
			OrderStatus oldStatus = o.getStatus();
			o.setStatus(OrderStatus.CANCELLED);
			o.setCancellationReason(reason);
			o.setCancelledDate(java.time.LocalDateTime.now());

			Order saved = orderRepository.save(o);
			// Notify status change
			sendOrderStatusChangeNotification(saved, oldStatus, OrderStatus.CANCELLED);

			return true;
        }).orElse(false);
    }

    /**
     * Delete order completely from database
     * This is used when payment is cancelled during checkout process
     * The order was never actually placed, so we don't keep a record
     */
    @CacheEvict(value = "orders", allEntries = true)
    @Transactional
    public void deleteOrder(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            logger.info("Deleting order {} due to payment cancellation during checkout", order.getOrderNumber());
            // Cascade delete will handle order items
            orderRepository.delete(order);
            logger.info("Order {} deleted successfully", order.getOrderNumber());
        });
    }

    /**
     * Get top selling products based on completed orders
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findTopProducts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        var stats = orderItemRepository.findTopProducts(PageRequest.of(0, safeLimit));

        return stats.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", s.getProductId());
            m.put("productName", s.getProductName());
            m.put("productImage", s.getProductImage());
            m.put("totalQuantity", s.getTotalQuantity());
            m.put("totalRevenue", s.getTotalRevenue());
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get category distribution (sales by category)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryDistribution() {
        try {
            // Get all completed order items grouped by product
            var stats = orderItemRepository.findTopProducts(PageRequest.of(0, 10000));
            
            // Map to store category revenue
            Map<String, java.math.BigDecimal> categoryRevenue = new HashMap<>();
            Map<String, String> productToCategory = new HashMap<>();
            
            // Fetch category for each product
            for (var stat : stats) {
                String productId = stat.getProductId();
                
                // Check cache first
                if (!productToCategory.containsKey(productId)) {
                    try {
                        // Call product service to get category
                        String url = productServiceUrl + "/api/products/" + productId;
                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        String credentials = interserviceUsername + ":" + interservicePassword;
                        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
                        headers.set("Authorization", "Basic " + encodedCredentials);
                        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
                        
                        @SuppressWarnings("rawtypes")
                        org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                            url, org.springframework.http.HttpMethod.GET, entity, Map.class);
                        
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            Map<?, ?> product = response.getBody();
                            if (product != null) {
                                Object categoryIdObj = product.get("categoryId");
                                
                                if (categoryIdObj != null) {
                                String categoryId = categoryIdObj.toString();
                                productToCategory.put(productId, categoryId);
                                
                                // Initialize category revenue if not exists
                                categoryRevenue.putIfAbsent(categoryId, java.math.BigDecimal.ZERO);
                                
                                // Add revenue to category
                                java.math.BigDecimal revenue = stat.getTotalRevenue();
                                if (revenue != null) {
                                    categoryRevenue.put(categoryId, categoryRevenue.get(categoryId).add(revenue));
                                }
                            }
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fetch category for product {}: {}", productId, e.getMessage());
                    }
                } else {
                    // Use cached category
                    String categoryId = productToCategory.get(productId);
                    categoryRevenue.putIfAbsent(categoryId, java.math.BigDecimal.ZERO);
                    java.math.BigDecimal revenue = stat.getTotalRevenue();
                    if (revenue != null) {
                        categoryRevenue.put(categoryId, categoryRevenue.get(categoryId).add(revenue));
                    }
                }
            }
            
            // Fetch category names and build result
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Map.Entry<String, java.math.BigDecimal> entry : categoryRevenue.entrySet()) {
                String categoryId = entry.getKey();
                java.math.BigDecimal revenue = entry.getValue();
                
                String categoryName = "Category " + categoryId;
                try {
                    // Fetch category name
                    String url = categoryServiceUrl + "/api/categories/" + categoryId;
                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    String credentials = interserviceUsername + ":" + interservicePassword;
                    String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
                    headers.set("Authorization", "Basic " + encodedCredentials);
                    org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
                    
                    @SuppressWarnings("rawtypes")
                    org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                        url, org.springframework.http.HttpMethod.GET, entity, Map.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<?, ?> category = response.getBody();
                        if (category != null) {
                            Object nameObj = category.get("name");
                            if (nameObj != null) {
                                categoryName = nameObj.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to fetch category name for {}: {}", categoryId, e.getMessage());
                }
                
                Map<String, Object> item = new HashMap<>();
                item.put("name", categoryName);
                item.put("value", revenue.doubleValue());
                result.add(item);
            }
            
            // Sort by revenue descending
            result.sort((a, b) -> Double.compare(
                ((Number) b.get("value")).doubleValue(),
                ((Number) a.get("value")).doubleValue()
            ));
            
            return result;
        } catch (Exception e) {
            logger.error("Error getting category distribution: {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
}


