package com.example.voucher.event;

import com.example.voucher.entity.VoucherUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class VoucherEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(VoucherEventPublisher.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String VOUCHER_TOPIC = "voucher-events";
    private static final String NOTIFICATION_TOPIC = "voucher-notifications";

    /**
     * Publish voucher applied event
     */
    public void publishVoucherAppliedEvent(String voucherId, String voucherCode, String userId) {
        try {
            VoucherEvent event = new VoucherEvent("VOUCHER_APPLIED", voucherId, voucherCode, userId, "CART");

            kafkaTemplate.send(VOUCHER_TOPIC, voucherCode, event);
            kafkaTemplate.send(NOTIFICATION_TOPIC, "voucher-applied", event);

            logger.info("Published VOUCHER_APPLIED event for voucher: {} by user: {}", voucherCode, userId);
        } catch (Exception e) {
            logger.error("Failed to publish VOUCHER_APPLIED event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish voucher used event
     */
    public void publishVoucherUsedEvent(VoucherUsage usage) {
        try {
            VoucherEvent event = new VoucherEvent("VOUCHER_USED", usage, "ORDER");

            kafkaTemplate.send(VOUCHER_TOPIC, usage.getVoucherCode(), event);
            kafkaTemplate.send(NOTIFICATION_TOPIC, "voucher-used", event);

            logger.info("Published VOUCHER_USED event for voucher: {} in order: {}",
                       usage.getVoucherCode(), usage.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to publish VOUCHER_USED event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish voucher expired event
     */
    public void publishVoucherExpiredEvent(String voucherId, String voucherCode) {
        try {
            VoucherEvent event = new VoucherEvent("VOUCHER_EXPIRED", voucherId, voucherCode, null, "SYSTEM");

            kafkaTemplate.send(VOUCHER_TOPIC, voucherCode, event);
            kafkaTemplate.send(NOTIFICATION_TOPIC, "voucher-expired", event);

            logger.info("Published VOUCHER_EXPIRED event for voucher: {}", voucherCode);
        } catch (Exception e) {
            logger.error("Failed to publish VOUCHER_EXPIRED event: {}", e.getMessage(), e);
        }
    }
}
