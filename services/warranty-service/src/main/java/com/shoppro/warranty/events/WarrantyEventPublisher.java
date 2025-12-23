package com.shoppro.warranty.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppro.warranty.entity.WarrantyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WarrantyEventPublisher {

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String WARRANTY_REQUESTED_TOPIC = "warranty-requested";
    private static final String WARRANTY_STATUS_UPDATED_TOPIC = "warranty-status-updated";

    /**
     * Publish warranty requested event
     */
    public void publishWarrantyRequestedEvent(WarrantyRequest warrantyRequest) {
        if (kafkaTemplate == null) {
            System.out.println("Kafka not available, skipping WARRANTY_REQUESTED event for request: " + warrantyRequest.getRequestNumber());
            return;
        }
        try {
            String eventData = objectMapper.writeValueAsString(warrantyRequest);
            kafkaTemplate.send(WARRANTY_REQUESTED_TOPIC, warrantyRequest.getRequestNumber(), eventData);
            System.out.println("Published WARRANTY_REQUESTED event for request: " + warrantyRequest.getRequestNumber());
        } catch (Exception e) {
            System.err.println("Failed to publish WARRANTY_REQUESTED event: " + e.getMessage());
        }
    }

    /**
     * Publish warranty status updated event
     */
    public void publishWarrantyStatusUpdatedEvent(WarrantyRequest warrantyRequest) {
        if (kafkaTemplate == null) {
            System.out.println("Kafka not available, skipping WARRANTY_STATUS_UPDATED event for request: " + warrantyRequest.getRequestNumber());
            return;
        }
        try {
            String eventData = objectMapper.writeValueAsString(warrantyRequest);
            kafkaTemplate.send(WARRANTY_STATUS_UPDATED_TOPIC, warrantyRequest.getRequestNumber(), eventData);
            System.out.println("Published WARRANTY_STATUS_UPDATED event for request: " + warrantyRequest.getRequestNumber());
        } catch (Exception e) {
            System.err.println("Failed to publish WARRANTY_STATUS_UPDATED event: " + e.getMessage());
        }
    }
}
