package com.example.notification.controller;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.EmailService;
import com.example.notification.service.NotificationService;
import com.example.shared.util.AuthUtils;
import com.example.shared.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    /**
     * Get notifications for user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserNotifications(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Validate user ID format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access (allow access if no authentication or if userId matches)
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty() && !authUserId.equals(userId)) {
                    if (!AuthUtils.isAdmin(authentication)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }
            
            List<Notification> notifications = notificationService.getUserNotifications(userId);
            List<Map<String, Object>> responses = notifications.stream()
                .map(this::createNotificationResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unread notifications for user
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Map<String, Object>>> getUserUnreadNotifications(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Validate user ID format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access (allow access if no authentication or if userId matches)
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty() && !authUserId.equals(userId)) {
                    if (!AuthUtils.isAdmin(authentication)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                }
            }
            
            List<Notification> notifications = notificationService.getUserUnreadNotifications(userId);
            List<Map<String, Object>> responses = notifications.stream()
                .map(this::createNotificationResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unread count for user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // If authentication is provided, use authenticated user's ID (ignore path parameter for security)
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId; // Use authenticated user's ID
                    logger.debug("Using authenticated user ID: {}", userId);
                }
            }
            
            // Validate user ID format (allow UUID, numeric, or alphanumeric like "a1")
            boolean isValidUUID = SecurityUtils.isValidUUID(userId);
            boolean isNumeric = isNumericId(userId);
            boolean isAlphanumeric = isAlphanumericId(userId);
            
            logger.debug("Validating user ID: {} - isValidUUID: {}, isNumeric: {}, isAlphanumeric: {}", 
                userId, isValidUUID, isNumeric, isAlphanumeric);
            
            if (!isValidUUID && !isNumeric && !isAlphanumeric) {
                logger.warn("Invalid user ID format: {}", userId);
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID format"));
            }
            
            // Verify user access (if authentication provided, must match)
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty() && !authUserId.equals(userId)) {
                    // Check if user is admin (admin can access any user's data)
                    if (!AuthUtils.isAdmin(authentication)) {
                        logger.warn("Access denied: userId {} does not match authenticated user {}", userId, authUserId);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
                    }
                }
            }
            
            Long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mark notification as read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read for user
     */
    @PostMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Validate user ID format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Invalid user ID format"));
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "Access denied"));
            }
            
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "All notifications marked as read"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "error", "Access denied"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Create notification
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String typeStr = (String) request.get("type");
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            String channel = (String) request.getOrDefault("channel", "websocket");
            Integer priority = (Integer) request.getOrDefault("priority", 1);
            
            NotificationType type = NotificationType.valueOf(typeStr.toUpperCase());
            
            Notification notification = notificationService.createNotification(
                userId, type, title, message, channel, priority
            );
            
            // Send notification asynchronously
            notificationService.sendNotification(notification);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "notificationId", notification.getId(),
                "message", "Notification created and queued for sending"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Handle order status change notification
     */
    @PostMapping("/order/status-change")
    public ResponseEntity<Map<String, Object>> handleOrderStatusChange(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String orderNumber = request.get("orderNumber").toString();
            String oldStatus = request.get("oldStatus").toString();
            String newStatus = request.get("newStatus").toString();
            Double totalAmount = request.get("totalAmount") != null ? 
                ((Number) request.get("totalAmount")).doubleValue() : null;
            String trackingNumber = request.get("trackingNumber") != null ? 
                request.get("trackingNumber").toString() : null;
            
            // Create appropriate notification based on status
            String title = "";
            String message = "";
            NotificationType type = NotificationType.ORDER_CONFIRMED;
            
            switch (newStatus.toUpperCase()) {
                case "CONFIRMED":
                    title = "✅ Đơn hàng đã được xác nhận";
                    message = String.format("Đơn hàng %s của bạn đã được xác nhận. Chúng tôi đang chuẩn bị hàng cho bạn!", orderNumber);
                    type = NotificationType.ORDER_CONFIRMED;
                    break;
                case "PROCESSING":
                    title = "📦 Đơn hàng đang được xử lý";
                    message = String.format("Đơn hàng %s của bạn đang được xử lý. Chúng tôi sẽ giao hàng sớm nhất có thể!", orderNumber);
                    type = NotificationType.ORDER_CONFIRMED;
                    break;
                case "SHIPPED":
                    title = "🚚 Đơn hàng đã được giao";
                    if (trackingNumber != null && !trackingNumber.isEmpty()) {
                        message = String.format("Đơn hàng %s của bạn đã được giao! Mã vận đơn: %s. Bạn có thể theo dõi đơn hàng trên website của đơn vị vận chuyển.", orderNumber, trackingNumber);
                    } else {
                        message = String.format("Đơn hàng %s của bạn đã được giao! Chúng tôi sẽ cập nhật mã vận đơn sớm nhất có thể.", orderNumber);
                    }
                    type = NotificationType.ORDER_SHIPPED;
                    break;
                case "DELIVERED":
                    title = "🎉 Đơn hàng đã được giao thành công";
                    message = String.format("Đơn hàng %s của bạn đã được giao thành công! Cảm ơn bạn đã mua sắm tại cửa hàng của chúng tôi. Hãy để lại đánh giá sản phẩm nhé!", orderNumber);
                    type = NotificationType.ORDER_DELIVERED;
                    break;
                case "CANCELLED":
                    title = "❌ Đơn hàng đã bị hủy";
                    message = String.format("Đơn hàng %s của bạn đã bị hủy. Nếu bạn có thắc mắc, vui lòng liên hệ bộ phận hỗ trợ.", orderNumber);
                    type = NotificationType.ORDER_CANCELLED;
                    break;
                default:
                    title = "📋 Cập nhật đơn hàng";
                    message = String.format("Đơn hàng %s của bạn đã được cập nhật trạng thái từ %s sang %s.", orderNumber, oldStatus, newStatus);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", request.get("orderId"));
            data.put("orderNumber", orderNumber);
            data.put("oldStatus", oldStatus);
            data.put("newStatus", newStatus);
            if (totalAmount != null) {
                data.put("totalAmount", totalAmount);
            }
            if (trackingNumber != null) {
                data.put("trackingNumber", trackingNumber);
            }
            
            Notification notification = notificationService.createNotification(
                userId, type, title, message, "websocket", 3
            );
            try {
                notification.setData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                logger.error("Error serializing notification data: {}", e.getMessage());
            }
            notificationRepository.save(notification);
            notificationService.sendNotification(notification);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Order status change notification sent"
            ));
        } catch (Exception e) {
            logger.error("Error handling order status change notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Send order notification with realtime broadcasting
     */
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> sendOrderNotification(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String orderNumber = (String) request.get("orderNumber");
            String status = (String) request.get("status");
            Object totalAmount = request.get("totalAmount");
            
            // Create notification
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(NotificationType.ORDER_CREATED);
            notification.setTitle("Order Update");
            notification.setMessage("Your order " + orderNumber + " status: " + status);
            notification.setData("{\"orderNumber\":\"" + orderNumber + "\",\"status\":\"" + status + "\",\"totalAmount\":" + (totalAmount != null ? totalAmount : 0) + "}");
            
            notificationService.createNotification(userId, NotificationType.ORDER_CREATED, "Order Update", 
                "Your order " + orderNumber + " status: " + status, 
                "websocket", 
                2);
            
            // Broadcast realtime notification via WebSocket
            broadcastRealtimeNotification(userId, notification);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Order notification sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Broadcast realtime notification via WebSocket
     */
    private void broadcastRealtimeNotification(String userId, Notification notification) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "notification");
            message.put("id", notification.getId());
            message.put("title", notification.getTitle());
            message.put("message", notification.getMessage());
            message.put("data", notification.getData());
            message.put("timestamp", notification.getCreatedAt());
            message.put("isRead", notification.getIsRead());
            
            // Send to user-specific queue
            messagingTemplate.convertAndSendToUser(
                userId, 
                "/queue/notifications", 
                message
            );
            
            // Also broadcast to general topic for admin monitoring
            messagingTemplate.convertAndSend("/topic/admin/notifications", message);
            
        } catch (Exception e) {
            // Log error but don't fail the notification
            System.err.println("Failed to broadcast realtime notification: " + e.getMessage());
        }
    }
    
    /**
     * Send payment notification with realtime broadcasting
     */
    @PostMapping("/payment")
    public ResponseEntity<Map<String, Object>> sendPaymentNotification(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String paymentId = request.get("paymentId").toString();
            String status = request.get("status").toString();
            Object amount = request.get("amount");
            String orderNumber = request.get("orderNumber") != null ? request.get("orderNumber").toString() : null;
            String transactionId = request.get("transactionId") != null ? request.get("transactionId").toString() : null;
            String paymentMethod = request.get("paymentMethod") != null ? request.get("paymentMethod").toString() : "UNKNOWN";
            
            // Create appropriate notification based on status
            String title = "";
            String message = "";
            NotificationType type = NotificationType.PAYMENT_SUCCESS;
            
            if ("SUCCESS".equalsIgnoreCase(status)) {
                title = "✅ Thanh toán thành công";
                if (orderNumber != null) {
                    message = String.format("Thanh toán cho đơn hàng %s đã thành công!\n\n", orderNumber);
                } else {
                    message = String.format("Thanh toán %s đã thành công!\n\n", paymentId);
                }
                if (transactionId != null) {
                    message += String.format("Mã giao dịch: %s\n", transactionId);
                }
                if (amount != null) {
                    double amountValue = amount instanceof Number ? ((Number) amount).doubleValue() : 0;
                    message += String.format("Số tiền: %,.0f VNĐ\n", amountValue);
                }
                message += "\nCảm ơn bạn đã mua sắm tại cửa hàng của chúng tôi!";
                type = NotificationType.PAYMENT_SUCCESS;
            } else {
                title = "❌ Thanh toán thất bại";
                if (orderNumber != null) {
                    message = String.format("Thanh toán cho đơn hàng %s đã thất bại.\n\n", orderNumber);
                } else {
                    message = String.format("Thanh toán %s đã thất bại.\n\n", paymentId);
                }
                message += "Vui lòng thử lại hoặc liên hệ bộ phận hỗ trợ nếu vấn đề vẫn tiếp tục.";
                type = NotificationType.PAYMENT_FAILED;
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("paymentId", paymentId);
            data.put("orderNumber", orderNumber);
            data.put("status", status);
            if (amount != null) {
                data.put("amount", amount instanceof Number ? ((Number) amount).doubleValue() : 0);
            }
            if (transactionId != null) {
                data.put("transactionId", transactionId);
            }
            data.put("paymentMethod", paymentMethod);
            
            Notification notification = notificationService.createNotification(
                userId, type, title, message, "websocket", 3
            );
            try {
                notification.setData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                logger.error("Error serializing notification data: {}", e.getMessage());
            }
            notificationRepository.save(notification);
            notificationService.sendNotification(notification);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Payment notification sent"));
        } catch (Exception e) {
            logger.error("Error sending payment notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }


    /**
     * Send promotional notification
     */
    @PostMapping("/promotional")
    public ResponseEntity<Map<String, Object>> sendPromotionalNotification(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            
            notificationService.sendPromotionalNotification(userId, title, message);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Promotional notification sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Send email verification email
     */
    @PostMapping("/email/verify")
    public ResponseEntity<Map<String, Object>> sendEmailVerification(@RequestBody Map<String, String> body) {
        try {
            String email = body.getOrDefault("email", "");
            String token = body.getOrDefault("token", "");
            String baseUrl = body.getOrDefault("baseUrl", "http://localhost:3000");

            if (email.isEmpty() || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "email and token are required"));
            }

            Map<String, Object> result = emailService.sendEmailVerificationEmail(email, token, baseUrl);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Verification email sent"));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", String.valueOf(result.get("error"))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Send password reset email
     */
    @PostMapping("/email/password-reset")
    public ResponseEntity<Map<String, Object>> sendPasswordResetEmail(@RequestBody Map<String, String> body) {
        try {
            String email = body.getOrDefault("email", "");
            String token = body.getOrDefault("token", "");
            String baseUrl = body.getOrDefault("baseUrl", "http://localhost:3000");

            if (email.isEmpty() || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "email and token are required"));
            }

            Map<String, Object> result = emailService.sendPasswordResetEmail(email, token, baseUrl);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Password reset email sent"));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", String.valueOf(result.get("error"))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Send welcome email
     */
    @PostMapping("/email/welcome")
    public ResponseEntity<Map<String, Object>> sendWelcomeEmail(@RequestBody Map<String, String> body) {
        try {
            String email = body.getOrDefault("email", "");
            String firstName = body.getOrDefault("firstName", "");
            String lastName = body.getOrDefault("lastName", "");

            if (email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "email is required"));
            }

            Map<String, Object> result = emailService.sendWelcomeEmail(email, firstName, lastName);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Welcome email sent"));
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", String.valueOf(result.get("error"))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all notifications with pagination
     */
    @GetMapping("")
    public ResponseEntity<Page<Map<String, Object>>> getAllNotifications(Pageable pageable) {
        try {
            Page<Notification> notifications = notificationService.getAllNotifications(pageable);
            Page<Map<String, Object>> responses = notifications.map(this::createNotificationResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get notifications by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Map<String, Object>>> getNotificationsByType(@PathVariable NotificationType type) {
        try {
            List<Notification> notifications = notificationService.getNotificationsByType(type);
            List<Map<String, Object>> responses = notifications.stream()
                .map(this::createNotificationResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get notification statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics() {
        try {
            Map<String, Object> stats = notificationService.getNotificationStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create notification response DTO
     */
    private Map<String, Object> createNotificationResponse(Notification notification) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", notification.getId());
        response.put("userId", notification.getUserId());
        response.put("type", notification.getType().name());
        response.put("title", notification.getTitle());
        response.put("message", notification.getMessage());
        response.put("data", notification.getData());
        response.put("isRead", notification.getIsRead());
        response.put("isSent", notification.getIsSent());
        response.put("sentAt", notification.getSentAt());
        response.put("readAt", notification.getReadAt());
        response.put("priority", notification.getPriority());
        response.put("channel", notification.getChannel());
        response.put("externalId", notification.getExternalId());
        response.put("retryCount", notification.getRetryCount());
        response.put("maxRetries", notification.getMaxRetries());
        response.put("errorMessage", notification.getErrorMessage());
        response.put("scheduledAt", notification.getScheduledAt());
        response.put("expiresAt", notification.getExpiresAt());
        response.put("createdAt", notification.getCreatedAt());
        response.put("updatedAt", notification.getUpdatedAt());
        
        return response;
    }
    
    /**
     * Helper method to check if string is numeric ID
     */
    private boolean isNumericId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Helper method to check if string is alphanumeric ID (e.g., "a1", "user123")
     */
    private boolean isAlphanumericId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        // Allow alphanumeric IDs (letters and numbers)
        return id.matches("^[a-zA-Z0-9]+$");
    }
}