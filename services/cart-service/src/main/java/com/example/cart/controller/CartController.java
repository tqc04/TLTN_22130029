package com.example.cart.controller;

import com.example.cart.dto.CartDTO;
import com.example.cart.service.CartService;
import com.example.shared.util.SecurityUtils;
import com.example.shared.util.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    
    @Autowired
    private CartService cartService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<CartDTO> getCart(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            if (!isSupportedUserId(userId)) {
                logger.warn("Invalid userId format: {}", userId);
                return ResponseEntity.badRequest().body(null);
            }
            
            // Verify user access (user can only access their own cart, admin can access any)
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CartDTO cart = cartService.getCart(userId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error getting cart: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    // Support query param: GET /api/cart?userId=1
    @GetMapping
    public ResponseEntity<CartDTO> getCartWithQuery(
            @RequestParam("userId") String userId,
            Authentication authentication) {
        try {
            logger.info("Get cart request - userId: {}", userId);
            
            if (!isSupportedUserId(userId)) {
                logger.warn("Invalid userId format: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CartDTO cart = cartService.getCart(userId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error getting cart: ", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/{userId}/add")
    public ResponseEntity<?> addItem(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            if (!isSupportedUserId(userId)) {
                logger.warn("Invalid userId format: {}", userId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid user ID format"));
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Access denied"));
            }
            
            if (request == null || !request.containsKey("productId") || !request.containsKey("quantity")) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Missing productId or quantity"));
            }
            
            String productId = request.get("productId").toString();
            
            // Validate productId UUID format
            if (!SecurityUtils.isValidUUID(productId)) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid product ID format"));
            }
            
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            
            // Support optional variantId
            String variantId = null;
            if (request.containsKey("variantId") && request.get("variantId") != null) {
                variantId = request.get("variantId").toString();
            }
            
            CartDTO cart = cartService.addItem(userId, productId, variantId, quantity);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse("Access denied"));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid UUID or number format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid ID or number format"));
        } catch (ResponseStatusException e) {
            logger.error("Service error: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                .body(createErrorResponse(e.getReason()));
        } catch (Exception e) {
            logger.error("Unexpected error adding item to cart: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    // Backward-compatible: support POST /api/cart/add?userId=... used by legacy frontend
    @PostMapping("/add")
    public ResponseEntity<?> addItemWithQuery(@RequestParam("userId") String userId, @RequestBody Map<String, Object> request) {
        try {
            logger.info("Add to cart request - userId: {}, request: {}", userId, request);
            String productId = request.get("productId").toString();
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            
            // Support optional variantId
            String variantId = null;
            if (request.containsKey("variantId") && request.get("variantId") != null) {
                variantId = request.get("variantId").toString();
                logger.info("Parsed - productId: {}, variantId: {}, quantity: {}", productId, variantId, quantity);
            } else {
                logger.info("Parsed - productId: {}, quantity: {}", productId, quantity);
            }
            
            CartDTO cart = cartService.addItem(userId, productId, variantId, quantity);
            logger.info("Cart service returned successfully");
            return ResponseEntity.ok(cart);
        } catch (NumberFormatException e) {
            logger.error("Invalid number format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid number format"));
        } catch (ResponseStatusException e) {
            logger.error("Service error: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                .body(createErrorResponse(e.getReason()));
        } catch (Exception e) {
            logger.error("Error adding item to cart: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    // Guest cart: allow adding without authentication using a sessionId
    @PostMapping("/guest/add")
    public ResponseEntity<?> addItemGuest(@RequestParam String sessionId, @RequestBody Map<String, Object> request) {
        try {
            String productId = request.get("productId").toString();
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            CartDTO cart = cartService.addItem("guest_" + sessionId, productId, quantity);
            return ResponseEntity.ok(cart);
        } catch (NumberFormatException e) {
            logger.error("Invalid number format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid number format"));
        } catch (ResponseStatusException e) {
            logger.error("Service error: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                .body(createErrorResponse(e.getReason()));
        } catch (Exception e) {
            logger.error("Error adding item to guest cart: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{userId}/update")
    public ResponseEntity<CartDTO> updateItemQuantity(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                    logger.info("Using userId from authentication: {}", userId);
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                logger.warn("Invalid userId format: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            String productId = request.get("productId").toString();
            
            // Validate productId format (allow UUID, numeric, or alphanumeric)
            if (!SecurityUtils.isValidUUID(productId) && !isNumericId(productId) && !isAlphanumericId(productId)) {
                return ResponseEntity.badRequest().build();
            }
            
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            
            CartDTO cart = cartService.updateItemQuantity(userId, productId, quantity);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error updating cart item: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{userId}/remove/{productId}")
    public ResponseEntity<CartDTO> removeItem(
            @PathVariable String userId,
            @PathVariable String productId,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                    logger.info("Using userId from authentication: {}", userId);
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                logger.warn("Invalid userId format: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // Validate productId format (allow UUID, numeric, or alphanumeric)
            if (!SecurityUtils.isValidUUID(productId) && !isNumericId(productId) && !isAlphanumericId(productId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CartDTO cart = cartService.removeItem(userId, productId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error removing cart item: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<CartDTO> clearCart(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                    logger.info("Using userId from authentication: {}", userId);
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                logger.warn("Invalid userId format: {}", userId);
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CartDTO cart = cartService.clearCart(userId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            logger.error("Error clearing cart: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{userId}/voucher")
    public ResponseEntity<CartDTO> applyVoucher(
            @PathVariable String userId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            if (!isSupportedUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            String voucherCode = request.get("voucherCode");
            CartDTO cart = cartService.applyVoucher(userId, voucherCode);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{userId}/voucher")
    public ResponseEntity<CartDTO> removeVoucher(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            if (!isSupportedUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            CartDTO cart = cartService.removeVoucher(userId);
            return ResponseEntity.ok(cart);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Integer>> getCartItemCount(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            if (!isSupportedUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            int count = cartService.getCartItemCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{userId}/empty")
    public ResponseEntity<Map<String, Boolean>> isCartEmpty(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            if (!isSupportedUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            boolean isEmpty = cartService.isCartEmpty(userId);
            return ResponseEntity.ok(Map.of("empty", isEmpty));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Cart API is working!");
    }
    
    /**
     * Helper method to check if string is numeric ID (for backward compatibility)
     */
    private boolean isNumericId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Helper method to check if string is alphanumeric ID (e.g., "a1", "user123")
     */
    private boolean isAlphanumericId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        // Allow alphanumeric IDs (letters and numbers)
        return id.matches("^[a-zA-Z0-9]+$");
    }
    
    private boolean isSupportedUserId(String userId) {
        return SecurityUtils.isValidUUID(userId) || isNumericId(userId) || isAlphanumericId(userId);
    }
    
    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("message", message);
        error.put("success", false);
        return error;
    }
}