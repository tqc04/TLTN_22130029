package com.example.notification.service;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired(required = false)
    private UserServiceClient userServiceClient;

    /**
     * Create notification
     */
    public Notification createNotification(String userId, NotificationType type, String title, 
                                         String message, String channel, Integer priority) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setChannel(channel);
        notification.setPriority(priority != null ? priority : 1);
        notification.setScheduledAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }

    /**
     * Send notification immediately
     */
    @Async
    public void sendNotification(Notification notification) {
        try {
            String channel = notification.getChannel();
            Map<String, Object> result = null;
            
            switch (channel.toLowerCase()) {
                case "email":
                    result = sendEmailNotification(notification);
                    break;
                case "sms":
                    result = sendSmsNotification(notification);
                    break;
                case "websocket":
                    result = sendWebSocketNotification(notification);
                    break;
                default:
                    result = Map.of("success", false, "error", "Unsupported channel: " + channel + ". Supported channels: email, sms, websocket");
            }
            
            if ((Boolean) result.get("success")) {
                notification.setIsSent(true);
                notification.setSentAt(LocalDateTime.now());
                notification.setExternalId((String) result.get("externalId"));
            } else {
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setErrorMessage((String) result.get("error"));
                
                if (notification.getRetryCount() >= notification.getMaxRetries()) {
                    notification.setIsSent(false);
                }
            }
            
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Send email notification
     */
    private Map<String, Object> sendEmailNotification(Notification notification) {
        try {
            // Get user email from user service
            String userEmail = getUserEmail(notification.getUserId());
            if (userEmail == null) {
                return Map.of("success", false, "error", "User email not found");
            }
            
            Map<String, Object> result = emailService.sendHtmlEmail(userEmail, notification.getTitle(), notification.getMessage());
            result.put("externalId", "EMAIL_" + System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send SMS notification
     */
    private Map<String, Object> sendSmsNotification(Notification notification) {
        try {
            String userPhone = getUserPhoneNumber(notification.getUserId());
            if (userPhone == null) {
                return Map.of("success", false, "error", "User phone number not found");
            }
            
            Map<String, Object> result = smsService.sendSms(userPhone, notification.getMessage());
            result.put("externalId", "SMS_" + System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Send WebSocket notification
     */
    private Map<String, Object> sendWebSocketNotification(Notification notification) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", notification.getId());
            data.put("type", notification.getType().name());
            data.put("title", notification.getTitle());
            data.put("message", notification.getMessage());
            data.put("priority", notification.getPriority());
            data.put("data", notification.getData());
            
            webSocketService.sendRealTimeNotification(
                notification.getUserId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                data
            );
            
            return Map.of("success", true, "externalId", "WS_" + System.currentTimeMillis());
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }


    /**
     * Process pending notifications
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void processPendingNotifications() {
        try {
            List<Notification> pendingNotifications = notificationRepository.findPendingNotifications(LocalDateTime.now());
            
            for (Notification notification : pendingNotifications) {
                sendNotification(notification);
            }
            
            logger.info("Processed {} pending notifications", pendingNotifications.size());
        } catch (Exception e) {
            logger.error("Error processing pending notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up expired notifications
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupExpiredNotifications() {
        try {
            List<Notification> expiredNotifications = notificationRepository.findExpiredNotifications(LocalDateTime.now());
            
            for (Notification notification : expiredNotifications) {
                notification.setIsSent(false);
                notification.setErrorMessage("Notification expired");
                notificationRepository.save(notification);
            }
            
            logger.info("Cleaned up {} expired notifications", expiredNotifications.size());
        } catch (Exception e) {
            logger.error("Error cleaning up expired notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Get notifications for user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for user
     */
    public List<Notification> getUserUnreadNotifications(String userId) {
        return notificationRepository.findUnreadByUserIdOrderByPriorityAndCreatedAt(userId);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    /**
     * Mark all notifications as read for user
     */
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsRead(userId, false);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Get notification count for user
     */
    public Long getUnreadCount(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Get all notifications with pagination
     */
    public Page<Notification> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }

    /**
     * Get notifications by type
     */
    public List<Notification> getNotificationsByType(NotificationType type) {
        return notificationRepository.findByTypeAndCreatedAtAfter(type, LocalDateTime.now().minusDays(30));
    }

    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Get notification statistics
     */
    public Map<String, Object> getNotificationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNotifications", notificationRepository.count());
        stats.put("sentNotifications", notificationRepository.countByIsSent(true));
        stats.put("pendingNotifications", notificationRepository.countByIsSent(false));
        stats.put("unreadNotifications", notificationRepository.countByIsRead(false));
        
        return stats;
    }

    /**
     * Send order notification
     */
    public void sendOrderNotification(String userId, String orderNumber, String status) {
        String title = "Order Update";
        String message = String.format("Your order %s status has been updated to %s", orderNumber, status);
        
        Notification notification = createNotification(userId, NotificationType.ORDER_CONFIRMED, title, message, "websocket", 2);
        sendNotification(notification);
        
        // Also send email for important updates
        if ("SHIPPED".equals(status) || "DELIVERED".equals(status)) {
            Notification emailNotification = createNotification(userId, NotificationType.ORDER_SHIPPED, title, message, "email", 2);
            sendNotification(emailNotification);
        }
    }

    /**
     * Send payment notification
     */
    public void sendPaymentNotification(String userId, String amount, String status) {
        String title = "Payment Update";
        String message = String.format("Your payment of %s has been %s", amount, status);
        
        NotificationType type = "SUCCESS".equals(status) ? NotificationType.PAYMENT_SUCCESS : NotificationType.PAYMENT_FAILED;
        Notification notification = createNotification(userId, type, title, message, "websocket", 3);
        sendNotification(notification);
    }

    /**
     * Send promotional notification
     */
    public void sendPromotionalNotification(String userId, String title, String message) {
        Notification notification = createNotification(userId, NotificationType.PROMOTION, title, message, "email", 1);
        sendNotification(notification);
    }

    /**
     * Create warranty request notification
     */
    public void createWarrantyRequestNotification(String userId, String requestNumber, String productName, String status) {
        String title = "Warranty Request Update";
        String message = String.format("Your warranty request %s for product %s has been %s", requestNumber, productName, status);

        Notification notification = createNotification(userId, NotificationType.WARRANTY_UPDATE, title, message, "websocket", 2);
        sendNotification(notification);
    }

    /**
     * Create warranty status update notification
     */
    public void createWarrantyStatusUpdateNotification(String userId, String requestNumber, String productName, String newStatus) {
        String title = "Warranty Status Updated";
        String message = String.format("Your warranty request %s for product %s status has been updated to %s", requestNumber, productName, newStatus);

        Notification notification = createNotification(userId, NotificationType.WARRANTY_UPDATE, title, message, "websocket", 2);
        sendNotification(notification);
    }

    /**
     * Create support notification for admin team
     */
    public void createSupportNotification(String name, String email, String subject, String category, String message) {
        // Create notification for admin users
        // In a real implementation, this would get admin user IDs from user service
        List<String> adminUserIds = getAdminUserIds();

        for (String adminId : adminUserIds) {
            String title = "New Support Message";
            String supportMessage = String.format("New support message from %s (%s):\n\nSubject: %s\nCategory: %s\nMessage: %s",
                name, email, subject, category, message);

            Notification notification = createNotification(adminId, NotificationType.SUPPORT, title, supportMessage, "websocket", 3);
            sendNotification(notification);
        }
    }

    /**
     * Send support confirmation email
     */
    @Async
    public void sendSupportConfirmationEmail(String email, String name, String subject) {
        try {
            String emailSubject = "Support Request Received - " + subject;
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for contacting us. We have received your support request with the subject: %s\n\n" +
                "Our support team will review your message and get back to you within 24 hours.\n\n" +
                "Best regards,\n" +
                "ShopPro Support Team",
                name, subject
            );

            emailService.sendSimpleEmail(email, emailSubject, emailBody);
        } catch (Exception e) {
            logger.error("Error sending support confirmation email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send warranty confirmation email
     */
    @Async
    public void sendWarrantyConfirmationEmail(String email, String name, String requestNumber, String productName) {
        try {
            String emailSubject = "Warranty Request Received - " + requestNumber;
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Thank you for submitting your warranty request for product: %s\n\n" +
                "Request Details:\n" +
                "- Request Number: %s\n" +
                "- Product: %s\n" +
                "- Status: Under Review\n\n" +
                "Our technical team will review your request and contact you within 2-3 business days.\n" +
                "You can track your warranty status at: https://shoppro.com/support/warranty/%s\n\n" +
                "Best regards,\n" +
                "ShopPro Warranty Team",
                name, productName, requestNumber, productName, requestNumber
            );

            emailService.sendSimpleEmail(email, emailSubject, emailBody);
        } catch (Exception e) {
            logger.error("Error sending warranty confirmation email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send warranty status update email
     */
    @Async
    public void sendWarrantyStatusUpdateEmail(String email, String name, String requestNumber, String productName, String status) {
        try {
            String emailSubject = "Warranty Status Update - " + requestNumber;
            String emailBody = String.format(
                "Dear %s,\n\n" +
                "Your warranty request for product %s has been updated.\n\n" +
                "Request Details:\n" +
                "- Request Number: %s\n" +
                "- Product: %s\n" +
                "- New Status: %s\n\n",
                name, productName, requestNumber, productName, status
            );

            // Add specific instructions based on status
            switch (status.toUpperCase()) {
                case "APPROVED":
                    emailBody += "Please bring your product to our service center or ship it to us using the prepaid shipping label we will send you.\n\n";
                    break;
                case "COMPLETED":
                    emailBody += "Your product has been repaired/replaced. Please visit our service center to collect your product.\n\n";
                    break;
                case "REJECTED":
                    emailBody += "Unfortunately, your warranty request does not meet our warranty criteria. Please contact support for more information.\n\n";
                    break;
            }

            emailBody += "You can view detailed information at: https://shoppro.com/support/warranty/" + requestNumber + "\n\n" +
                        "Best regards,\n" +
                        "ShopPro Warranty Team";

            emailService.sendSimpleEmail(email, emailSubject, emailBody);
        } catch (Exception e) {
            logger.error("Error sending warranty status update email: {}", e.getMessage(), e);
        }
    }

    /**
     * Get admin user IDs from user service
     */
    private List<String> getAdminUserIds() {
        try {
            if (userServiceClient == null) {
                logger.warn("UserServiceClient not available, returning empty admin list");
                return List.of();
            }
            
            List<Map<String, Object>> adminUsers = userServiceClient.getAdminUsers();
            return adminUsers.stream()
                    .filter(user -> user.containsKey("id"))
                    .map(user -> user.get("id").toString())
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to get admin user IDs from user service: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Get user email from user service
     */
    private String getUserEmail(String userId) {
        try {
            if (userServiceClient == null) {
                logger.warn("UserServiceClient not available for userId: {}", userId);
                return null;
            }
            
            Map<String, Object> user = userServiceClient.getUserById(userId);
            if (user != null && user.containsKey("email")) {
                return user.get("email").toString();
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to get user email for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Get user phone number from user service
     */
    private String getUserPhoneNumber(String userId) {
        try {
            if (userServiceClient == null) {
                logger.warn("UserServiceClient not available for userId: {}", userId);
                return null;
            }
            
            Map<String, Object> user = userServiceClient.getUserById(userId);
            if (user != null && user.containsKey("phoneNumber")) {
                Object phoneNumber = user.get("phoneNumber");
                return phoneNumber != null ? phoneNumber.toString() : null;
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to get user phone number for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
