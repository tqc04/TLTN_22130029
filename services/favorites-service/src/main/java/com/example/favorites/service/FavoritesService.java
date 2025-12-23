package com.example.favorites.service;

import com.example.favorites.entity.Favorite;
import com.example.favorites.repository.FavoritesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FavoritesService {
    
    private static final Logger logger = LoggerFactory.getLogger(FavoritesService.class);

    @Autowired
    private FavoritesRepository favoritesRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.product.base-url:http://product-service}")
    private String productServiceUrl;
    
    @Value("${services.user.base-url:http://user-service}")
    private String userServiceUrl;
    
    /**
     * Get user's favorites
     */
    public List<Favorite> getUserFavorites(String userId) {
        return favoritesRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get user's favorites with pagination
     */
    public Page<Favorite> getUserFavoritesPage(String userId, Pageable pageable) {
        return favoritesRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    /**
     * Add product to favorites
     */
    public Favorite addToFavorites(String userId, String productId) {
        // Check if already in favorites
        if (favoritesRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new RuntimeException("Product already in favorites");
        }
        
        // Validate user exists
        validateUserExists(userId);
        
        // Get product information
        Map<String, Object> product = getProductInfo(productId);
        
        // Create favorite
        Favorite favorite = new Favorite(userId, productId);
        favorite.setProductName((String) product.get("name"));
        favorite.setProductPrice(((Number) product.get("price")).doubleValue());
        favorite.setProductImage((String) product.get("imageUrl"));
        
        return favoritesRepository.save(favorite);
    }
    
    /**
     * Remove product from favorites
     */
    public void removeFromFavorites(String userId, String productId) {
        Optional<Favorite> favorite = favoritesRepository.findByUserIdAndProductId(userId, productId);
        if (favorite.isPresent()) {
            favoritesRepository.delete(favorite.get());
        } else {
            throw new RuntimeException("Product not found in favorites");
        }
    }
    
    /**
     * Check if product is in favorites
     */
    public boolean isInFavorites(String userId, String productId) {
        return favoritesRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    /**
     * Get favorites count for user
     */
    public long getFavoritesCount(String userId) {
        return favoritesRepository.countByUserId(userId);
    }
    
    /**
     * Clear all favorites for user
     */
    public void clearFavorites(String userId) {
        favoritesRepository.deleteByUserId(userId);
    }
    
    /**
     * Get all favorites (for internal services)
     */
    public List<Favorite> getAllFavorites() {
        return favoritesRepository.findAll();
    }
    
    /**
     * Get favorite by ID
     */
    public Favorite getFavoriteById(Long id) {
        Optional<Favorite> favorite = favoritesRepository.findById(id);
        return favorite.orElse(null);
    }
    
    /**
     * Get most favorited products
     */
    public List<Map<String, Object>> getMostFavoritedProducts(int limit) {
        List<Object[]> results = favoritesRepository.findMostFavoritedProducts(limit);
        return results.stream()
                .map(row -> {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("productId", row[0]);
                    productMap.put("productName", row[1]);
                    productMap.put("productPrice", row[2]);
                    productMap.put("productImage", row[3]);
                    productMap.put("favoriteCount", row[4]);
                    return productMap;
                })
                .toList();
    }
    
    /**
     * Get recent favorites
     */
    public List<Favorite> getRecentFavorites(String userId, int limit) {
        return favoritesRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .limit(limit)
            .toList();
    }
    
    /**
     * Search favorites by product name
     */
    public List<Favorite> searchFavorites(String userId, String query) {
        return favoritesRepository.findByUserIdAndProductNameContainingIgnoreCase(userId, query);
    }
    
    /**
     * Validate user exists
     */
    private void validateUserExists(String userId) {
        try {
            restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, Map.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("User not found: " + userId);
            }

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                logger.warn("FavoritesService: unable to validate user {} due to {} (likely missing service auth). Allowing operation to proceed.", userId, e.getStatusCode());
                return;
            }

            logger.error("FavoritesService: error validating user {} - {}", userId, e.getStatusCode(), e);
            throw new RuntimeException("Failed to validate user: " + e.getStatusCode());
        } catch (Exception e) {
            logger.error("FavoritesService: unexpected error while validating user {}", userId, e);
            throw new RuntimeException("Failed to validate user: " + userId);
        }
    }
    
    /**
     * Get product information
     */
    private Map<String, Object> getProductInfo(String productId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) restTemplate.getForObject(
                productServiceUrl + "/api/products/" + productId, Map.class);
            
            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }
            
            return product;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch product information: " + e.getMessage());
        }
    }
}
