package com.example.order.task;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.PaymentStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to cleanup orders that are stuck in PROCESSING status
 * (e.g., user started payment but never completed it)
 * 
 * This task runs every 10 minutes and cancels orders that have been in PROCESSING
 * status for more than 30 minutes, releasing reserved inventory.
 */
@Component
public class PaymentTimeoutCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(PaymentTimeoutCleanupTask.class);
    
    // Timeout: 30 minutes
    private static final int PAYMENT_TIMEOUT_MINUTES = 30;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Cleanup orders that are stuck in PROCESSING status
     * Runs every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes = 600000 milliseconds
    @Transactional
    public void cleanupTimeoutOrders() {
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
            
            // Find orders that are in PROCESSING status and created more than 30 minutes ago
            // We need to load order items for inventory rollback
            List<Order> timeoutOrders = orderRepository.findByStatusAndPaymentStatusAndCreatedAtBefore(
                OrderStatus.PROCESSING,
                PaymentStatus.PROCESSING,
                timeoutThreshold
            );
            
            if (timeoutOrders.isEmpty()) {
                logger.debug("No timeout orders found for cleanup");
                return;
            }
            
            logger.info("Found {} orders stuck in PROCESSING status, cleaning up...", timeoutOrders.size());
            
            for (Order order : timeoutOrders) {
                try {
                    logger.warn("Cleaning up timeout order: {} (created at: {})", 
                        order.getOrderNumber(), order.getCreatedAt());
                    
                    // Load order items if not already loaded (for inventory rollback)
                    if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
                        // Try to load order with items
                        orderRepository.findByIdWithOrderItems(order.getId()).ifPresent(loadedOrder -> {
                            order.setOrderItems(loadedOrder.getOrderItems());
                        });
                    }
                    
                    // Rollback inventory reservation
                    if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                        orderService.rollbackInventoryForOrder(order);
                    }
                    
                    // Delete the order (since payment was never completed)
                    orderService.deleteOrder(order.getId());
                    
                    logger.info("Successfully cleaned up timeout order: {}", order.getOrderNumber());
                } catch (Exception e) {
                    logger.error("Failed to cleanup timeout order {}: {}", 
                        order.getOrderNumber(), e.getMessage(), e);
                    // Continue with next order
                }
            }
            
            logger.info("Payment timeout cleanup completed. Processed {} orders", timeoutOrders.size());
        } catch (Exception e) {
            logger.error("Error in payment timeout cleanup task: {}", e.getMessage(), e);
        }
    }
}

