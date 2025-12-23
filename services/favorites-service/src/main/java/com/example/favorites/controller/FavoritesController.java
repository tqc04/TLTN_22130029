package com.example.favorites.controller;

import com.example.favorites.entity.Favorite;
import com.example.favorites.service.FavoritesService;
import com.example.shared.util.SecurityUtils;
import com.example.shared.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FavoritesController {
    
    @Autowired
    private FavoritesService favoritesService;
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Favorites API is working!");
    }
    
    /**
     * Get user's favorites
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Favorite>> getUserFavorites(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Favorite> favorites = favoritesService.getUserFavorites(userId);
            return ResponseEntity.ok(favorites);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get user's favorites with pagination
     */
    @GetMapping("/user/{userId}/page")
    public ResponseEntity<Page<Favorite>> getUserFavoritesPage(
            @PathVariable String userId,
            Pageable pageable,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Page<Favorite> favorites = favoritesService.getUserFavoritesPage(userId, pageable);
            return ResponseEntity.ok(favorites);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Add product to favorites
     * Simple logic: Only need to be logged in, userId is taken from authentication
     */
    @PostMapping("/user/{userId}/add")
    public ResponseEntity<?> addToFavorites(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            // REQUIRE AUTHENTICATION - User must be logged in to add favorites
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required", "message", "You must be logged in to add products to favorites"));
            }
            
            // Get userId from authentication (user who is logged in)
            String authenticatedUserId = AuthUtils.extractUserIdFromAuth(authentication);
            String authenticatedUsername = authentication.getName();
            
            // Use authenticated user's ID, fallback to username if userId not found
            String actualUserId = authenticatedUserId;
            if (actualUserId == null || actualUserId.isEmpty()) {
                actualUserId = authenticatedUsername;
            }
            
            if (actualUserId == null || actualUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication", "message", "Unable to determine user ID from authentication"));
            }
            
            // Validate request body
            if (request == null || !request.containsKey("productId")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "productId is required", "message", "Request body must contain productId"));
            }
            
            Object productIdObj = request.get("productId");
            if (productIdObj == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "productId cannot be null", "message", "productId is required"));
            }
            
            String productId = productIdObj.toString();
            
            // Validate productId format (allow UUID, numeric, or alphanumeric)
            if (!SecurityUtils.isValidUUID(productId) && !isNumericId(productId) && !isAlphanumericId(productId)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid product ID format", "message", "Product ID must be a valid UUID, numeric, or alphanumeric format"));
            }
            
            // Add to favorites using authenticated user's ID
            // Note: userId from path is ignored, we use the authenticated user's ID
            Favorite favorite = favoritesService.addToFavorites(actualUserId, productId);
            return ResponseEntity.ok(favorite);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid ID format", "message", e.getMessage()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle database constraint violations (e.g., incorrect column type)
            // This must be caught before RuntimeException since it extends RuntimeException
            e.printStackTrace();
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            // Check if it's a column type mismatch (user_id INT vs VARCHAR)
            if (errorMessage != null && (errorMessage.contains("user_id") || errorMessage.contains("Incorrect integer value"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Database schema error", 
                        "message", "Database column type mismatch. Please run migration V2 to update user_id column to VARCHAR. Error: " + errorMessage));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Database constraint violation", "message", errorMessage != null ? errorMessage : "Failed to save favorite"));
        } catch (org.springframework.dao.DataAccessException e) {
            // Handle other database errors (e.g., SQL exceptions)
            // This must be caught before RuntimeException since it extends RuntimeException
            e.printStackTrace();
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Database error", "message", errorMessage != null ? errorMessage : "Failed to save favorite"));
        } catch (RuntimeException e) {
            // Handle business logic errors (e.g., already in favorites, user not found)
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        } catch (Exception e) {
            // Log the exception for debugging
            e.printStackTrace();
            String errorMessage = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred";
            if (e.getCause() != null) {
                errorMessage += " (Cause: " + e.getCause().getMessage() + ")";
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "message", errorMessage));
        }
    }
    
    /**
     * Remove product from favorites
     */
    @DeleteMapping("/user/{userId}/remove/{productId}")
    public ResponseEntity<Map<String, String>> removeFromFavorites(
            @PathVariable String userId,
            @PathVariable String productId,
            Authentication authentication) {
        try {
            // REQUIRE AUTHENTICATION - User must be logged in to remove favorites
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required", "message", "You must be logged in to remove products from favorites"));
            }
            
            // Extract userId from authentication (use authenticated user's ID)
            String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
            if (authUserId == null || authUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authentication", "message", "Unable to extract user ID from authentication token"));
            }
            
            // Use authenticated user's ID (ignore userId from path for security)
            userId = authUserId;
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Validate productId format (allow UUID, numeric, or alphanumeric)
            if (!SecurityUtils.isValidUUID(productId) && !isNumericId(productId) && !isAlphanumericId(productId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access (user can only remove from their own favorites, admin can remove from any)
            if (!AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            favoritesService.removeFromFavorites(userId, productId);
            return ResponseEntity.ok(Map.of("message", "Product removed from favorites"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Check if product is in favorites
     */
    @GetMapping("/user/{userId}/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> isInFavorites(
            @PathVariable String userId,
            @PathVariable String productId,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
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
            
            boolean isFavorite = favoritesService.isInFavorites(userId, productId);
            return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get favorites count for user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Map<String, Long>> getFavoritesCount(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            long count = favoritesService.getFavoritesCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Clear all favorites for user
     */
    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<Map<String, String>> clearFavorites(
            @PathVariable String userId,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            favoritesService.clearFavorites(userId);
            return ResponseEntity.ok(Map.of("message", "All favorites cleared"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get favorite by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Favorite> getFavorite(@PathVariable Long id) {
        try {
            Favorite favorite = favoritesService.getFavoriteById(id);
            if (favorite != null) {
                return ResponseEntity.ok(favorite);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get most favorited products
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> getMostFavoritedProducts(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> popularProducts = favoritesService.getMostFavoritedProducts(limit);
            return ResponseEntity.ok(popularProducts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Search products in favorites
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<Favorite>> searchFavorites(
            @PathVariable String userId,
            @RequestParam String query,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Favorite> favorites = favoritesService.searchFavorites(userId, query);
            return ResponseEntity.ok(favorites);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get recently added favorites
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Favorite>> getRecentFavorites(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            // Try to get userId from authentication if provided
            if (authentication != null) {
                String authUserId = AuthUtils.extractUserIdFromAuth(authentication);
                if (authUserId != null && !authUserId.isEmpty()) {
                    userId = authUserId;
                }
            }
            
            // Validate userId format (allow UUID, numeric, or alphanumeric like "a1")
            if (!SecurityUtils.isValidUUID(userId) && !isNumericId(userId) && !isAlphanumericId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Verify user access
            if (authentication != null && !AuthUtils.canAccessUserData(userId, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            List<Favorite> recentFavorites = favoritesService.getRecentFavorites(userId, limit);
            return ResponseEntity.ok(recentFavorites);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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
     * Get all favorites (for internal services like notification service)
     * This endpoint should be secured and only accessible by internal services
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllFavorites() {
        try {
            List<Favorite> allFavorites = favoritesService.getAllFavorites();
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("data", allFavorites);
            response.put("count", allFavorites.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
}
