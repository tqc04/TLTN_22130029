package com.example.product.scheduled;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to automatically end product sales when sale period expires
 * Professional e-commerce: Products return to original price when sale ends
 */
@Component
public class ProductSaleExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ProductSaleExpirationScheduler.class);

    @Autowired
    private ProductRepository productRepository;

    /**
     * Cleanup expired product sales once per day (optional - for database consistency)
     * Note: Products are automatically filtered via query based on real-time comparison
     * This task is only for database cleanup/maintenance, not for hiding products
     */
    @Scheduled(cron = "0 0 3 * * ?") // Once per day at 3:00 AM
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void cleanupExpiredProductSales() {
        try {
            LocalDateTime now = LocalDateTime.now();
            logger.info("Running daily cleanup for expired product sales at: {}", now);

            // Find products with expired sales for database consistency
            // Queries automatically filter based on real-time, so this is just for DB cleanup
            List<Product> expiredSaleProducts = productRepository.findExpiredSaleProducts();

            if (!expiredSaleProducts.isEmpty()) {
                logger.info("Found {} products with expired sales for cleanup", expiredSaleProducts.size());

                int updatedCount = 0;
                for (Product product : expiredSaleProducts) {
                    try {
                        if (product.getIsOnSale() != null && product.getIsOnSale()) {
                            product.setIsOnSale(false);
                            product.setUpdatedAt(now);
                            productRepository.save(product);
                            updatedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("Error in cleanup for product {}: {}", 
                            product.getId(), e.getMessage(), e);
                    }
                }

                logger.info("Cleanup completed: {}/{} products updated", 
                    updatedCount, expiredSaleProducts.size());
            }

        } catch (Exception e) {
            logger.error("Error in product sale cleanup scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Activate product sales when sale start date is reached
     * Note: Queries automatically show products on sale based on real-time comparison
     * This task is only for database consistency, not for showing products
     */
    @Scheduled(cron = "0 0 3 * * ?") // Once per day at 3:00 AM
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void activateScheduledProductSales() {
        try {
            LocalDateTime now = LocalDateTime.now();
            logger.debug("Checking for scheduled product sales to activate at: {}", now);

            // Find products that should be on sale but aren't yet
            List<Product> productsToActivate = productRepository.findAll().stream()
                .filter(product -> {
                    // Product has salePrice but isOnSale is false
                    if (product.getSalePrice() == null) {
                        return false;
                    }
                    if (product.getIsOnSale() != null && product.getIsOnSale()) {
                        return false; // Already on sale
                    }
                    
                    // Check if sale start date has been reached
                    if (product.getSaleStartAt() == null) {
                        return false; // No start date
                    }
                    
                    // Sale should start if current time is after or equal to saleStartAt
                    // and sale hasn't ended yet
                    boolean startDateReached = now.isAfter(product.getSaleStartAt()) || 
                                             now.isEqual(product.getSaleStartAt());
                    boolean notExpired = product.getSaleEndAt() == null || 
                                        now.isBefore(product.getSaleEndAt());
                    
                    return startDateReached && notExpired;
                })
                .toList();

            if (!productsToActivate.isEmpty()) {
                logger.info("Found {} products with scheduled sales to activate", productsToActivate.size());

                int activatedCount = 0;
                for (Product product : productsToActivate) {
                    try {
                        product.setIsOnSale(true);
                        product.setUpdatedAt(now);
                        productRepository.save(product);
                        
                        activatedCount++;
                        logger.info("Activated sale for product: {} (ID: {}) - Sale started at: {}, Sale price: {}", 
                            product.getName(), 
                            product.getId(),
                            product.getSaleStartAt(),
                            product.getSalePrice());
                    } catch (Exception e) {
                        logger.error("Error activating sale for product {}: {}", 
                            product.getId(), e.getMessage(), e);
                    }
                }

                logger.info("Successfully activated sales for {}/{} products", 
                    activatedCount, productsToActivate.size());
            } else {
                logger.debug("No scheduled product sales to activate");
            }

        } catch (Exception e) {
            logger.error("Error in product sale activation scheduler: {}", e.getMessage(), e);
        }
    }
}

