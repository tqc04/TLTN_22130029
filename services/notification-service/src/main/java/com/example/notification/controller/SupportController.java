package com.example.notification.controller;

import com.example.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Send contact/support message
     */
    @PostMapping("/contact")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> sendContactMessage(@RequestBody Map<String, String> contactData) {
        try {
            String name = contactData.get("name");
            String email = contactData.get("email");
            String subject = contactData.get("subject");
            String category = contactData.get("category");
            String message = contactData.get("message");

            if (name == null || email == null || subject == null || message == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Missing required fields"
                ));
            }

            // Create notification for support team
            notificationService.createSupportNotification(name, email, subject, category, message);

            // Send confirmation email to customer
            notificationService.sendSupportConfirmationEmail(email, name, subject);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tin nhắn của bạn đã được gửi thành công! Chúng tôi sẽ phản hồi sớm nhất có thể."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Có lỗi xảy ra khi gửi tin nhắn: " + e.getMessage()
            ));
        }
    }

    /**
     * Send warranty request notification (internal use)
     */
    @PostMapping("/warranty/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> sendWarrantyRequestNotification(@RequestBody Map<String, Object> warrantyData) {
        try {
            String userId = warrantyData.get("userId").toString();
            String requestNumber = (String) warrantyData.get("requestNumber");
            String productName = (String) warrantyData.get("productName");
            String customerEmail = (String) warrantyData.get("customerEmail");
            String customerName = (String) warrantyData.get("customerName");

            // Create in-app notification
            notificationService.createWarrantyRequestNotification(userId, requestNumber, productName, "created");

            // Send confirmation email
            notificationService.sendWarrantyConfirmationEmail(customerEmail, customerName, requestNumber, productName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Warranty request notification sent successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to send warranty notification: " + e.getMessage()
            ));
        }
    }

    /**
     * Send warranty status update notification (internal use)
     */
    @PostMapping("/warranty/status-update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> sendWarrantyStatusUpdateNotification(@RequestBody Map<String, Object> warrantyData) {
        try {
            String userId = warrantyData.get("userId").toString();
            String requestNumber = (String) warrantyData.get("requestNumber");
            String productName = (String) warrantyData.get("productName");
            String customerEmail = (String) warrantyData.get("customerEmail");
            String customerName = (String) warrantyData.get("customerName");
            String status = (String) warrantyData.get("status");

            // Create in-app notification
            notificationService.createWarrantyStatusUpdateNotification(userId, requestNumber, productName, status);

            // Send status update email
            notificationService.sendWarrantyStatusUpdateEmail(customerEmail, customerName, requestNumber, productName, status);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Warranty status update notification sent successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to send warranty status update notification: " + e.getMessage()
            ));
        }
    }
}
