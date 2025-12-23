package com.example.recommendation.controller;

import com.example.recommendation.dto.ProductRecommendation;
import com.example.recommendation.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Recommendation Controller
 * 
 * Endpoints:
 * - POST /api/recommendations/behavior - Track user behavior
 * - GET /api/recommendations/personalized/{userId} - Get personalized recommendations (CF + CBF)
 * - GET /api/recommendations/similar/{productId} - Get similar products (CBF)
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    
    @Autowired
    private RecommendationService recommendationService;
    
    /**
     * Track user behavior for Collaborative Filtering
     * 
     * Actions: view, add_to_cart, purchase, favorite, etc.
     */
    @PostMapping("/behavior")
    public ResponseEntity<Map<String, String>> trackUserBehavior(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String productId = request.get("productId").toString();
            String action = (String) request.get("action"); // view, add_to_cart, purchase, etc.
            
            recommendationService.trackUserBehavior(userId, productId, action);
            return ResponseEntity.ok(Map.of("message", "Behavior tracked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get personalized recommendations using AI
     * 
     * Uses:
     * 1. Collaborative Filtering (CF) - Based on similar users' preferences
     * 2. Content-Based Filtering (CBF) - Based on product similarity
     */
    @GetMapping("/personalized/{userId}")
    public ResponseEntity<List<ProductRecommendation>> getPersonalizedRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ProductRecommendation> recommendations = recommendationService.getPersonalizedRecommendations(userId, limit);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get similar products using Content-Based Filtering
     * 
     * Compares: category, brand, price, description
     */
    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<ProductRecommendation>> getSimilarProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ProductRecommendation> recommendations = recommendationService.getSimilarProducts(productId, limit);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Recommendation API is working!");
    }
}
