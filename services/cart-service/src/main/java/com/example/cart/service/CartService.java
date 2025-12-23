package com.example.cart.service;

import com.example.cart.dto.CartDTO;
import com.example.cart.dto.CartItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {
    
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private RedisTemplate<String, CartDTO> cartRedisTemplate;
    
    @Value("${services.product.base-url:http://localhost:8083}")
    private String productServiceUrl;
    
    @Value("${services.voucher.base-url:http://localhost:8092}")
    private String voucherServiceUrl;
    
    @Value("${services.inventory.base-url:http://localhost:8093}")
    private String inventoryServiceUrl;
    
    // Cart expiration time (24 hours)
    private static final long CART_EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    
    /**
     * Get cart for authenticated user
     */
    public CartDTO getCart(String userId) {
        String cartKey = "user_" + userId;
        CartDTO cart = cartRedisTemplate.opsForValue().get(cartKey);
        
        if (cart == null) {
            cart = new CartDTO(userId);
            cartRedisTemplate.opsForValue().set(cartKey, cart, Duration.ofMillis(CART_EXPIRATION_TIME));
        }
        
        // Enrich cart items with current stock quantities
        return enrichCartWithStock(cart);
    }
    
    /**
     * Get cart for guest user
     */
    public CartDTO getGuestCart(String sessionId) {
        String cartKey = "guest_" + sessionId;
        CartDTO cart = cartRedisTemplate.opsForValue().get(cartKey);
        
        if (cart == null) {
            cart = new CartDTO(null);
            cartRedisTemplate.opsForValue().set(cartKey, cart, Duration.ofMillis(CART_EXPIRATION_TIME));
        }
        
        return cart;
    }
    
    /**
     * Add item to cart (overloaded for backward compatibility)
     */
    public CartDTO addItem(String userId, String productId, Integer quantity) {
        return addItem(userId, productId, null, quantity);
    }
    
    /**
     * Add item to cart with optional variant support
     */
    public CartDTO addItem(String userId, String productId, String variantId, Integer quantity) {
        try {
            // Validate input parameters
            if (productId == null || productId.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product ID");
            }
            if (quantity == null || quantity <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
            }
            
            // Get product information from Product Service with timeout and error handling
            Map<String, Object> productResponse = null;
            String productEndpoint;
            
            if (variantId != null && !variantId.isEmpty()) {
                // Fetch variant information
                productEndpoint = productServiceUrl + "/api/products/variants/" + variantId;
                logger.info("Fetching variant info from: {}", productEndpoint);
            } else {
                // Fetch product information
                productEndpoint = productServiceUrl + "/api/products/" + productId;
                logger.info("Fetching product info from: {}", productEndpoint);
            }
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> productResp = (Map<String, Object>) restTemplate.getForObject(productEndpoint, Map.class);
                productResponse = productResp;
            } catch (Exception e) {
                logger.error("Failed to fetch product/variant from product service: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable");
            }
            
            if (productResponse == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, variantId != null ? "Product variant not found" : "Product not found");
            }
            
            // Check inventory with error handling
            Map<String, Object> inventoryResponse = null;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> invResp = (Map<String, Object>) restTemplate.getForObject(
                    inventoryServiceUrl + "/api/inventory/check-stock?productId=" + productId + "&quantity=" + quantity, Map.class);
                inventoryResponse = invResp;
            } catch (Exception e) {
                logger.warn("Failed to check inventory for product {}: {}", productId, e.getMessage());
                // Continue without inventory check if service is down
            }
            
            if (inventoryResponse != null && !Boolean.TRUE.equals(inventoryResponse.get("inStock"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
            }
            
            CartDTO cart = getCart(userId);
            
            // Check if item already exists
            Optional<CartItemDTO> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
            
            if (existingItem.isPresent()) {
                // Update quantity
                CartItemDTO item = existingItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                // Update stock quantity
                item.setStockQuantity(getStockQuantity(productId));
            } else {
                // Add new item
                // Get price: ưu tiên salePrice nếu có, nếu không thì dùng price
                BigDecimal itemPrice;
                if (productResponse.containsKey("salePrice") && productResponse.get("salePrice") != null) {
                    itemPrice = new BigDecimal(productResponse.get("salePrice").toString());
                } else {
                    itemPrice = new BigDecimal(productResponse.get("price").toString());
                }
                
                CartItemDTO newItem = new CartItemDTO(
                    productId,
                    (String) productResponse.get("name"),
                    getProductImage(productResponse),
                    itemPrice,
                    quantity
                );
                // Set stock quantity
                newItem.setStockQuantity(getStockQuantity(productId));
                cart.getItems().add(newItem);
            }
            
            // Recalculate totals
            recalculateCart(cart);
            cart.setUpdatedAt(LocalDateTime.now());
            
            saveCart(userId != null ? "user_" + userId : cartKeyFromCart(cart, null), cart);
            return enrichCartWithStock(cart);
            
        } catch (ResponseStatusException ex) {
            // Propagate as-is so the correct HTTP status is returned
            throw ex;
        } catch (Exception e) {
            logger.error("Error adding item to cart: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add item to cart");
        }
    }
    
    /**
     * Update item quantity in cart
     */
    public CartDTO updateItemQuantity(String userId, String productId, Integer quantity) {
        if (productId == null || productId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product ID");
        }
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
        }

        CartDTO cart = getCart(userId);

        Optional<CartItemDTO> itemOpt = cart.getItems().stream()
            .filter(item -> item.getProductId().equals(productId))
            .findFirst();

        if (itemOpt.isEmpty()) {
            // Nothing to update
            return cart;
        }

        // If new quantity is <= 0, remove item
        if (quantity <= 0) {
            cart.getItems().remove(itemOpt.get());
        } else {
            // Check stock with inventory service before updating
            try {
                Map<?, ?> inventoryResponse = restTemplate.getForObject(
                    inventoryServiceUrl + "/api/inventory/check-stock?productId=" + productId + "&quantity=" + quantity, Map.class);
                if (inventoryResponse != null && Boolean.FALSE.equals(inventoryResponse.get("inStock"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
                }
            } catch (ResponseStatusException ex) {
                throw ex;
            } catch (Exception e) {
                logger.warn("Inventory service unavailable when updating cart item {}: {}. Proceeding optimistically.", productId, e.getMessage());
            }
            CartItemDTO item = itemOpt.get();
            item.setQuantity(quantity);
            // Update stock quantity
            item.setStockQuantity(getStockQuantity(productId));
        }

        recalculateCart(cart);
        cart.setUpdatedAt(LocalDateTime.now());

        saveCart(userId != null ? "user_" + userId : cartKeyFromCart(cart, null), cart);
        return enrichCartWithStock(cart);
    }
    
    /**
     * Get stock quantity for a product from inventory service
     */
    private Integer getStockQuantity(String productId) {
        try {
            Map<String, Object> stockResponse = restTemplate.getForObject(
                inventoryServiceUrl + "/api/inventory/stock-quantity/" + productId, Map.class);
            if (stockResponse != null && stockResponse.containsKey("quantity")) {
                Object quantityObj = stockResponse.get("quantity");
                if (quantityObj instanceof Number) {
                    return ((Number) quantityObj).intValue();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get stock quantity for product {}: {}", productId, e.getMessage());
            // Fallback to product service
            try {
                Map<String, Object> productResponse = restTemplate.getForObject(
                    productServiceUrl + "/api/products/" + productId, Map.class);
                if (productResponse != null && productResponse.containsKey("stockQuantity")) {
                    Object stockObj = productResponse.get("stockQuantity");
                    if (stockObj instanceof Number) {
                        return ((Number) stockObj).intValue();
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to get stock from product service for product {}: {}", productId, ex.getMessage());
            }
        }
        return null; // Return null if unable to fetch stock
    }
    
    /**
     * Enrich cart items with current stock quantities
     * Always refreshes stock to ensure data is up-to-date
     */
    private CartDTO enrichCartWithStock(CartDTO cart) {
        if (cart == null || cart.getItems() == null) {
            return cart;
        }
        
        for (CartItemDTO item : cart.getItems()) {
            // Always refresh stock quantity to ensure data is current
            item.setStockQuantity(getStockQuantity(item.getProductId()));
        }
        
        return cart;
    }
    
    /**
     * Remove item from cart
     */
    public CartDTO removeItem(String userId, String productId) {
        CartDTO cart = getCart(userId);
        
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        
        recalculateCart(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        
        saveCart("user_" + userId, cart);
        return cart;
    }
    
    /**
     * Clear cart
     */
    public CartDTO clearCart(String userId) {
        CartDTO cart = getCart(userId);
        cart.getItems().clear();
        cart.setVoucherCode(null);
        
        recalculateCart(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        
        saveCart("user_" + userId, cart);
        return cart;
    }
    
    /**
     * Apply voucher to cart with detailed validation
     */
    public CartDTO applyVoucher(String userId, String voucherCode) {
        try {
            CartDTO cart = getCart(userId);

            // Prepare voucher validation request
            Map<String, Object> validationRequest = new HashMap<>();
            validationRequest.put("voucherCode", voucherCode);
            validationRequest.put("userId", userId);
            validationRequest.put("orderAmount", cart.getSubtotal());

            // Prepare items for validation
            List<Map<String, Object>> items = cart.getItems().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productId", item.getProductId());
                    itemMap.put("productName", item.getProductName());
                    itemMap.put("categoryId", 1L); 
                    itemMap.put("brandId", 1L);   
                    itemMap.put("price", item.getPrice());
                    itemMap.put("quantity", item.getQuantity());
                    return itemMap;
                })
                .collect(Collectors.toList());

            validationRequest.put("items", items);

            // Validate voucher with Voucher Service
            @SuppressWarnings("unchecked")
            Map<String, Object> voucherResponse = (Map<String, Object>) restTemplate.postForObject(
                voucherServiceUrl + "/api/vouchers/validate",
                validationRequest,
                Map.class);

            if (voucherResponse != null && Boolean.TRUE.equals(voucherResponse.get("valid"))) {
                cart.setVoucherCode(voucherCode);
                cart.setVoucherId(Long.valueOf(voucherResponse.get("voucherId").toString()));

                // Apply discount
                BigDecimal discountAmount = new BigDecimal(voucherResponse.get("discountAmount").toString());
                cart.setDiscount(discountAmount);

                // Store additional voucher info
                cart.setVoucherMessage((String) voucherResponse.get("message"));

                recalculateCart(cart);
                cart.setUpdatedAt(LocalDateTime.now());

                saveCart("user_" + userId, cart);
                logger.info("Successfully applied voucher {} to cart for user {}, discount: {}",
                           voucherCode, userId, discountAmount);

                return cart;
            } else {
                String errorMessage = voucherResponse != null ?
                    (String) voucherResponse.get("message") : "Invalid voucher code";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
            }

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("Error applying voucher: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to apply voucher: " + e.getMessage());
        }
    }
    
    /**
     * Remove voucher from cart
     */
    public CartDTO removeVoucher(String userId) {
        CartDTO cart = getCart(userId);
        cart.setVoucherCode(null);
        cart.setDiscount(BigDecimal.ZERO);
        
        recalculateCart(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        
        saveCart("user_" + userId, cart);
        return cart;
    }
    
    /**
     * Recalculate cart totals
     */
    private void recalculateCart(CartDTO cart) {
        BigDecimal subtotal = cart.getItems().stream()
            .map(CartItemDTO::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        cart.setSubtotal(subtotal);
        
        // Calculate tax (10%)
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.1"));
        cart.setTax(tax);
        
        // Calculate shipping (free if subtotal > 500000)
        BigDecimal shipping = subtotal.compareTo(new BigDecimal("500000")) > 0 ? 
            BigDecimal.ZERO : new BigDecimal("30000");
        cart.setShipping(shipping);
        
        // Calculate total
        BigDecimal total = subtotal.add(tax).add(shipping).subtract(cart.getDiscount());
        cart.setTotal(total);
    }

    private void saveCart(String key, CartDTO cart) {
        cartRedisTemplate.opsForValue().set(key, cart, Duration.ofMillis(CART_EXPIRATION_TIME));
    }

    private String cartKeyFromCart(CartDTO cart, String sessionId) {
        if (cart.getUserId() != null) {
            return "user_" + cart.getUserId();
        }
        return "guest_" + (sessionId != null ? sessionId : UUID.randomUUID().toString());
    }
    
    /**
     * Get product image from product response
     * Priority: 1) imageUrl (main product image), 2) images[0].imageUrl (first image in array)
     */
    private String getProductImage(Map<String, Object> productResponse) {
        try {
            // First try to get main imageUrl from product
            Object imageUrlObj = productResponse.get("imageUrl");
            if (imageUrlObj != null && !imageUrlObj.toString().trim().isEmpty()) {
                return imageUrlObj.toString();
            }
            
            // Fallback to images array
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> images = (List<Map<String, Object>>) productResponse.get("images");
            if (images != null && !images.isEmpty()) {
                Object firstImageUrl = images.get(0).get("imageUrl");
                if (firstImageUrl != null && !firstImageUrl.toString().trim().isEmpty()) {
                    return firstImageUrl.toString();
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing product image: {}", e.getMessage());
        }
        return "";
    }
    
    /**
     * Get cart item count
     */
    public int getCartItemCount(String userId) {
        CartDTO cart = getCart(userId);
        return cart.getItems().stream()
            .mapToInt(CartItemDTO::getQuantity)
            .sum();
    }
    
    /**
     * Check if cart is empty
     */
    public boolean isCartEmpty(String userId) {
        CartDTO cart = getCart(userId);
        return cart.getItems().isEmpty();
    }
}