package com.example.notification.service;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to check wishlist products for price drops and stock availability
 */
@Service
@Transactional
public class WishlistNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(WishlistNotificationService.class);
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${services.favorites.base-url:http://localhost:8087}")
    private String favoritesServiceUrl;
    
    @Value("${services.product.base-url:http://localhost:8082}")
    private String productServiceUrl;
    
    // Track last checked prices to avoid duplicate notifications
    private final Map<String, BigDecimal> lastCheckedPrices = new HashMap<>();
    private final Map<String, Integer> lastCheckedStock = new HashMap<>();
    
    /**
     * Check wishlist products for price drops and stock availability
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void checkWishlistProducts() {
        try {
            logger.info("Starting wishlist product check for price drops and stock...");
            
            // Get all favorites from favorites service
            List<Map<String, Object>> allFavorites = getAllFavorites();
            
            if (allFavorites == null || allFavorites.isEmpty()) {
                logger.debug("No favorites found to check");
                return;
            }
            
            int priceDropCount = 0;
            int stockAvailableCount = 0;
            
            for (Map<String, Object> favorite : allFavorites) {
                String userId = (String) favorite.get("userId");
                String productId = (String) favorite.get("productId");
                String productName = (String) favorite.get("productName");
                Double savedPrice = favorite.get("productPrice") != null ? 
                    ((Number) favorite.get("productPrice")).doubleValue() : null;
                
                if (userId == null || productId == null) {
                    continue;
                }
                
                // Get current product details
                Map<String, Object> product = getProductDetails(productId);
                if (product == null) {
                    continue;
                }
                
                // Check for price drop
                // Logic: Compare current price with last checked price (not savedPrice in favorite)
                // This ensures we only notify when price drops from the last check, not from when user added to favorite
                BigDecimal currentPrice = getCurrentPrice(product);
                if (currentPrice != null) {
                    String priceKey = userId + "_" + productId;
                    BigDecimal lastCheckedPrice = lastCheckedPrices.get(priceKey);
                    
                    // If this is first check, use savedPrice from favorite as baseline
                    if (lastCheckedPrice == null && savedPrice != null) {
                        lastCheckedPrice = BigDecimal.valueOf(savedPrice);
                    }
                    
                    // Only notify if price dropped compared to last check (with minimum 5% threshold)
                    if (lastCheckedPrice != null && currentPrice.compareTo(lastCheckedPrice) < 0) {
                        BigDecimal priceDrop = lastCheckedPrice.subtract(currentPrice);
                        double discountPercent = (priceDrop.doubleValue() / lastCheckedPrice.doubleValue()) * 100;
                        
                        // Only notify if discount is at least 5% to avoid spam
                        if (discountPercent >= 5.0) {
                            sendPriceDropNotification(userId, productId, productName, lastCheckedPrice, currentPrice, discountPercent);
                            lastCheckedPrices.put(priceKey, currentPrice);
                            priceDropCount++;
                        } else {
                            // Update last checked price even if discount is too small
                            lastCheckedPrices.put(priceKey, currentPrice);
                        }
                    } else {
                        // Update last checked price even if no drop or price increased
                        lastCheckedPrices.put(priceKey, currentPrice);
                    }
                }
                
                // Check for stock availability
                Integer currentStock = getCurrentStock(product);
                String stockKey = userId + "_" + productId;
                Integer lastStock = lastCheckedStock.get(stockKey);
                
                if (currentStock != null && currentStock > 0) {
                    if (lastStock == null || lastStock == 0) {
                        // Stock just became available!
                        sendStockAvailableNotification(userId, productId, productName, currentStock);
                        stockAvailableCount++;
                    }
                    lastCheckedStock.put(stockKey, currentStock);
                } else {
                    lastCheckedStock.put(stockKey, 0);
                }
            }
            
            logger.info("Wishlist check completed. Price drops: {}, Stock available: {}", 
                priceDropCount, stockAvailableCount);
                
        } catch (Exception e) {
            logger.error("Error checking wishlist products: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get all favorites from favorites service
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAllFavorites() {
        try {
            String url = favoritesServiceUrl + "/api/favorites/all";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            
            // Fallback: try to get favorites by user (would need to iterate users)
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching favorites: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Get product details from product service
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getProductDetails(String productId) {
        try {
            String url = productServiceUrl + "/api/products/" + productId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (Exception e) {
            logger.debug("Error fetching product {}: {}", productId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get current price (salePrice if available, otherwise price)
     */
    private BigDecimal getCurrentPrice(Map<String, Object> product) {
        try {
            Object salePrice = product.get("salePrice");
            Object price = product.get("price");
            
            if (salePrice != null) {
                if (salePrice instanceof Number) {
                    return BigDecimal.valueOf(((Number) salePrice).doubleValue());
                }
            }
            
            if (price != null && price instanceof Number) {
                return BigDecimal.valueOf(((Number) price).doubleValue());
            }
            
            return null;
        } catch (Exception e) {
            logger.debug("Error extracting price: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get current stock quantity
     */
    private Integer getCurrentStock(Map<String, Object> product) {
        try {
            Object stock = product.get("stockQuantity");
            if (stock != null && stock instanceof Number) {
                return ((Number) stock).intValue();
            }
            return 0;
        } catch (Exception e) {
            logger.debug("Error extracting stock: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Send price drop notification
     */
    private void sendPriceDropNotification(String userId, String productId, String productName, 
                                          BigDecimal oldPrice, BigDecimal newPrice, double discountPercent) {
        try {
            String title = "🎉 Sản phẩm yêu thích đang giảm giá!";
            String message = String.format(
                "Sản phẩm \"%s\" trong danh sách yêu thích của bạn đang giảm giá %.0f%%!\n\n" +
                "💰 Giá cũ: %,.0f VNĐ\n" +
                "💰 Giá mới: %,.0f VNĐ\n" +
                "💵 Tiết kiệm: %,.0f VNĐ\n\n" +
                "Nhanh tay mua ngay để không bỏ lỡ ưu đãi!",
                productName,
                discountPercent,
                oldPrice.doubleValue(),
                newPrice.doubleValue(),
                oldPrice.subtract(newPrice).doubleValue()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("productName", productName);
            data.put("oldPrice", oldPrice.doubleValue());
            data.put("newPrice", newPrice.doubleValue());
            data.put("discountPercent", discountPercent);
            data.put("discountAmount", oldPrice.subtract(newPrice).doubleValue());
            
            Notification notification = notificationService.createNotification(
                userId,
                NotificationType.PRICE_DROP,
                title,
                message,
                "websocket",
                3 // High priority
            );
            
            try {
                notification.setData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                logger.error("Error serializing notification data: {}", e.getMessage());
            }
            notificationRepository.save(notification);
            notificationService.sendNotification(notification);
            
            logger.info("Price drop notification sent to user {} for product {}", userId, productId);
        } catch (Exception e) {
            logger.error("Error sending price drop notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send stock available notification
     */
    private void sendStockAvailableNotification(String userId, String productId, String productName, Integer stock) {
        try {
            String title = "✅ Sản phẩm yêu thích đã có hàng!";
            String message = String.format(
                "Sản phẩm \"%s\" trong danh sách yêu thích của bạn đã có hàng trở lại!\n\n" +
                "📦 Số lượng còn lại: %d sản phẩm\n\n" +
                "Nhanh tay đặt hàng ngay để không bỏ lỡ!",
                productName,
                stock
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("productId", productId);
            data.put("productName", productName);
            data.put("stockQuantity", stock);
            
            Notification notification = notificationService.createNotification(
                userId,
                NotificationType.PRODUCT_IN_STOCK,
                title,
                message,
                "websocket",
                2 // Medium priority
            );
            
            try {
                notification.setData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                logger.error("Error serializing notification data: {}", e.getMessage());
            }
            notificationRepository.save(notification);
            notificationService.sendNotification(notification);
            
            logger.info("Stock available notification sent to user {} for product {}", userId, productId);
        } catch (Exception e) {
            logger.error("Error sending stock available notification: {}", e.getMessage(), e);
        }
    }
}

