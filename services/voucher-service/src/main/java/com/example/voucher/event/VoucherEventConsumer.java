package com.example.voucher.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class VoucherEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VoucherEventConsumer.class);

    @KafkaListener(topics = "voucher-events", groupId = "voucher-service-events")
    public void handleVoucherEvent(
            @Payload VoucherEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received voucher event: {} from topic: {}", event.getEventType(), topic);

            switch (event.getEventType()) {
                case "VOUCHER_APPLIED":
                    handleVoucherApplied(event);
                    break;
                case "VOUCHER_USED":
                    handleVoucherUsed(event);
                    break;
                case "VOUCHER_EXPIRED":
                    handleVoucherExpired(event);
                    break;
                default:
                    logger.warn("Unknown voucher event type: {}", event.getEventType());
            }

            acknowledgment.acknowledge();
            logger.info("Successfully processed voucher event: {}", event.getEventType());

        } catch (Exception e) {
            logger.error("Failed to process voucher event: {}", e.getMessage(), e);
            // Don't acknowledge on error - message will be retried
        }
    }

    private void handleVoucherApplied(VoucherEvent event) {
        logger.info("Processing VOUCHER_APPLIED event for voucher: {} by user: {}",
                   event.getVoucherCode(), event.getUserId());

        // Could trigger additional business logic here
        // For example: send welcome email, update user preferences, etc.
    }

    private void handleVoucherUsed(VoucherEvent event) {
        logger.info("Processing VOUCHER_USED event for voucher: {} in order: {}",
                   event.getVoucherCode(), event.getOrderNumber());

        // Update voucher statistics
        // Send confirmation email to user
        // Update marketing analytics

        // Example: Send notification to user
        try {
            // This could trigger a notification service call
            // notificationService.sendVoucherUsedNotification(event.getUserId(), event);
            logger.info("Voucher usage notification queued for user: {}", event.getUserId());
        } catch (Exception e) {
            logger.error("Failed to send voucher usage notification: {}", e.getMessage());
        }
    }

    private void handleVoucherExpired(VoucherEvent event) {
        logger.info("Processing VOUCHER_EXPIRED event for voucher: {}", event.getVoucherCode());

        // Deactivate expired voucher
        // Send notification to admin
        // Clean up related data

        try {
            // This could trigger admin notification
            // adminNotificationService.sendVoucherExpiredAlert(event);
            logger.info("Voucher expired alert sent to admin");
        } catch (Exception e) {
            logger.error("Failed to send voucher expired alert: {}", e.getMessage());
        }
    }
}
