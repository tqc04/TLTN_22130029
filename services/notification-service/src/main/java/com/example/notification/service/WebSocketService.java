package com.example.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to specific user
     */
    public void sendNotificationToUser(String userId, Map<String, Object> notification) {
        String destination = "/user/" + userId + "/queue/notifications";
        messagingTemplate.convertAndSend(destination, notification);
    }

    /**
     * Send notification to all users
     */
    public void sendNotificationToAll(Map<String, Object> notification) {
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    /**
     * Send order update notification
     */
    public void sendOrderUpdateNotification(String userId, String orderNumber, String status) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "ORDER_UPDATE");
        notification.put("orderNumber", orderNumber);
        notification.put("status", status);
        notification.put("message", "Order " + orderNumber + " status updated to " + status);
        notification.put("timestamp", System.currentTimeMillis());
        
        sendNotificationToUser(userId, notification);
    }

    /**
     * Send payment notification
     */
    public void sendPaymentNotification(String userId, String amount, String status) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PAYMENT_UPDATE");
        notification.put("amount", amount);
        notification.put("status", status);
        notification.put("message", "Payment " + status + " for amount " + amount);
        notification.put("timestamp", System.currentTimeMillis());
        
        sendNotificationToUser(userId, notification);
    }

    /**
     * Send product notification
     */
    public void sendProductNotification(String userId, String productName, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PRODUCT_UPDATE");
        notification.put("productName", productName);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        
        sendNotificationToUser(userId, notification);
    }

    /**
     * Send system notification
     */
    public void sendSystemNotification(String message, String level) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "SYSTEM_NOTIFICATION");
        notification.put("message", message);
        notification.put("level", level);
        notification.put("timestamp", System.currentTimeMillis());
        
        sendNotificationToAll(notification);
    }

    /**
     * Send promotional notification
     */
    public void sendPromotionalNotification(String userId, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PROMOTIONAL");
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        
        sendNotificationToUser(userId, notification);
    }

    /**
     * Send real-time notification
     */
    public void sendRealTimeNotification(String userId, String type, String title, String message, Map<String, Object> data) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("data", data);
        notification.put("timestamp", System.currentTimeMillis());
        
        sendNotificationToUser(userId, notification);
    }
}
