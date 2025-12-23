package com.example.notification.service;

import com.example.notification.entity.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class WarrantyEventConsumer {

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String WARRANTY_REQUESTED_TOPIC = "warranty-requested";
    private static final String WARRANTY_STATUS_UPDATED_TOPIC = "warranty-status-updated";

    /**
     * Consume warranty requested events
     */
    @KafkaListener(topics = WARRANTY_REQUESTED_TOPIC, groupId = "notification-service-warranty-group", autoStartup = "${kafka.enabled:true}")
    public void consumeWarrantyRequestedEvent(String message) {
        try {
            System.out.println("Received WARRANTY_REQUESTED event: " + message);

            JsonNode warrantyRequest = objectMapper.readTree(message);

            // Send confirmation email to customer
            sendWarrantyRequestConfirmationEmail(warrantyRequest);

            // Create in-app notification
            createWarrantyRequestNotification(warrantyRequest);

        } catch (Exception e) {
            System.err.println("Error processing WARRANTY_REQUESTED event: " + e.getMessage());
        }
    }

    /**
     * Consume warranty status updated events
     */
    @KafkaListener(topics = WARRANTY_STATUS_UPDATED_TOPIC, groupId = "notification-service-warranty-group", autoStartup = "${kafka.enabled:true}")
    public void consumeWarrantyStatusUpdatedEvent(String message) {
        try {
            System.out.println("Received WARRANTY_STATUS_UPDATED event: " + message);

            JsonNode warrantyRequest = objectMapper.readTree(message);

            // Send status update email to customer
            sendWarrantyStatusUpdateEmail(warrantyRequest);

            // Create in-app notification
            createWarrantyStatusUpdateNotification(warrantyRequest);

        } catch (Exception e) {
            System.err.println("Error processing WARRANTY_STATUS_UPDATED event: " + e.getMessage());
        }
    }

    /**
     * Send warranty request confirmation email
     */
    private void sendWarrantyRequestConfirmationEmail(JsonNode warrantyRequest) {
        try {
            String customerEmail = warrantyRequest.get("customerEmail").asText();
            String customerName = warrantyRequest.get("customerName").asText();
            String requestNumber = warrantyRequest.get("requestNumber").asText();
            String productName = warrantyRequest.get("productName").asText();

            String subject = "Xác nhận yêu cầu bảo hành - " + requestNumber;

            String body = String.format(
                "Kính gửi %s,\n\n" +
                "Cảm ơn bạn đã gửi yêu cầu bảo hành cho sản phẩm %s.\n\n" +
                "Thông tin yêu cầu bảo hành:\n" +
                "- Mã yêu cầu: %s\n" +
                "- Sản phẩm: %s\n" +
                "- Trạng thái: Đang xử lý\n\n" +
                "Chúng tôi sẽ xử lý yêu cầu của bạn trong thời gian sớm nhất.\n" +
                "Bạn có thể theo dõi trạng thái bảo hành tại: https://shoppro.com/support/warranty/%s\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ ShopPro",
                customerName, productName, requestNumber, productName, requestNumber
            );

            emailService.sendSimpleEmail(customerEmail, subject, body);

        } catch (Exception e) {
            System.err.println("Error sending warranty confirmation email: " + e.getMessage());
        }
    }

    /**
     * Send warranty status update email
     */
    private void sendWarrantyStatusUpdateEmail(JsonNode warrantyRequest) {
        try {
            String customerEmail = warrantyRequest.get("customerEmail").asText();
            String customerName = warrantyRequest.get("customerName").asText();
            String requestNumber = warrantyRequest.get("requestNumber").asText();
            String productName = warrantyRequest.get("productName").asText();
            String status = warrantyRequest.get("status").asText();

            String statusText = getStatusText(status);
            String subject = "Cập nhật trạng thái bảo hành - " + requestNumber;

            String body = String.format(
                "Kính gửi %s,\n\n" +
                "Yêu cầu bảo hành của bạn cho sản phẩm %s đã được cập nhật.\n\n" +
                "Thông tin cập nhật:\n" +
                "- Mã yêu cầu: %s\n" +
                "- Sản phẩm: %s\n" +
                "- Trạng thái mới: %s\n\n",
                customerName, productName, requestNumber, productName, statusText
            );

            // Add specific instructions based on status
            switch (status.toUpperCase()) {
                case "APPROVED":
                    body += "Vui lòng mang sản phẩm đến cửa hàng hoặc gửi qua đường bưu điện theo hướng dẫn.\n\n";
                    break;
                case "COMPLETED":
                    body += "Sản phẩm của bạn đã được sửa chữa/bảo hành xong. Vui lòng đến nhận sản phẩm.\n\n";
                    break;
                case "REJECTED":
                    String rejectionReason = warrantyRequest.has("rejectionReason")
                        ? warrantyRequest.get("rejectionReason").asText()
                        : "Không đủ điều kiện bảo hành";
                    body += "Lý do: " + rejectionReason + "\n\n";
                    break;
            }

            body += "Bạn có thể xem chi tiết tại: https://shoppro.com/support/warranty/" + requestNumber + "\n\n" +
                   "Trân trọng,\n" +
                   "Đội ngũ ShopPro";

            emailService.sendSimpleEmail(customerEmail, subject, body);

        } catch (Exception e) {
            System.err.println("Error sending warranty status update email: " + e.getMessage());
        }
    }

    /**
     * Create in-app notification for warranty request
     */
    private void createWarrantyRequestNotification(JsonNode warrantyRequest) {
        try {
            String userId = warrantyRequest.get("userId").asText();
            String requestNumber = warrantyRequest.get("requestNumber").asText();
            String productName = warrantyRequest.get("productName").asText();

            String title = "Yêu cầu bảo hành đã được tạo";
            String message = String.format("Yêu cầu bảo hành cho sản phẩm %s (Mã: %s) đã được ghi nhận và đang xử lý.",
                                          productName, requestNumber);

            notificationService.createNotification(userId, NotificationType.WARRANTY_UPDATE, title, message, "websocket", 2);

        } catch (Exception e) {
            System.err.println("Error creating warranty request notification: " + e.getMessage());
        }
    }

    /**
     * Create in-app notification for warranty status update
     */
    private void createWarrantyStatusUpdateNotification(JsonNode warrantyRequest) {
        try {
            String userId = warrantyRequest.get("userId").asText();
            String requestNumber = warrantyRequest.get("requestNumber").asText();
            String productName = warrantyRequest.get("productName").asText();
            String status = warrantyRequest.get("status").asText();

            String title = "Cập nhật trạng thái bảo hành";
            String message = String.format("Trạng thái bảo hành cho sản phẩm %s (Mã: %s) đã được cập nhật thành: %s",
                                          productName, requestNumber, getStatusText(status));

            notificationService.createNotification(userId, NotificationType.WARRANTY_UPDATE, title, message, "websocket", 2);

        } catch (Exception e) {
            System.err.println("Error creating warranty status update notification: " + e.getMessage());
        }
    }

    /**
     * Convert status enum to Vietnamese text
     */
    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "PENDING": return "Đang chờ xử lý";
            case "APPROVED": return "Đã duyệt";
            case "RECEIVED": return "Đã nhận sản phẩm";
            case "IN_PROGRESS": return "Đang sửa chữa";
            case "COMPLETED": return "Hoàn thành";
            case "REJECTED": return "Từ chối";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }
}
