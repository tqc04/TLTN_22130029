package com.example.review.controller;

import com.example.review.entity.ProductReview;
import com.example.review.service.ReviewService;
import com.example.shared.util.AuthUtils;
import com.example.shared.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.user.base-url:http://localhost:8082}")
    private String userServiceUrl;
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Review API is working!");
    }
    
    /**
     * Get all reviews with pagination
     */
    @GetMapping("")
    public ResponseEntity<Page<ProductReview>> getAllReviews(Pageable pageable) {
        try {
            Page<ProductReview> reviews = reviewService.getAllReviews(pageable);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get all reviews for admin with filters (Admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<Page<Map<String, Object>>> getAllReviewsForAdmin(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Boolean isApproved,
            @RequestParam(required = false) Boolean isSpam,
            @RequestParam(required = false) Integer rating,
            Pageable pageable) {
        try {
            Page<ProductReview> reviews = reviewService.getAllReviewsForAdmin(
                productId, userId, isApproved, isSpam, rating, pageable);
            
            // Enrich with user information
            List<Map<String, Object>> enrichedReviews = enrichReviewsWithUserInfo(reviews.getContent());
            
            // Create new page with enriched content
            Page<Map<String, Object>> enrichedPage = new org.springframework.data.domain.PageImpl<>(
                enrichedReviews, 
                reviews.getPageable(), 
                reviews.getTotalElements()
            );
            
            return ResponseEntity.ok(enrichedPage);
        } catch (Exception e) {
            logger.error("Error getting reviews for admin", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get review statistics for admin (Admin only)
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    public ResponseEntity<Map<String, Object>> getAdminReviewStats() {
        try {
            Map<String, Object> stats = reviewService.getAdminReviewStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting review stats", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get reviews for a specific product with user information
     * Supports pagination via query parameters (page, size, sortBy, sortDir)
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductReviews(
            @PathVariable String productId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        try {
            // If pagination params are provided, use paginated endpoint
            if (page > 0 || size != 10) {
                org.springframework.data.domain.Sort.Direction direction = 
                    sortDir.equalsIgnoreCase("desc") ? org.springframework.data.domain.Sort.Direction.DESC 
                    : org.springframework.data.domain.Sort.Direction.ASC;
                org.springframework.data.domain.Pageable pageable = 
                    org.springframework.data.domain.PageRequest.of(page, size, 
                        org.springframework.data.domain.Sort.by(direction, sortBy));
                
                Page<ProductReview> reviewsPage = reviewService.getAllReviews(pageable);
                // Filter by productId
                List<ProductReview> filteredReviews = reviewsPage.getContent().stream()
                    .filter(r -> r.getProductId().equals(productId))
                    .collect(java.util.stream.Collectors.toList());
                
                List<Map<String, Object>> enrichedReviews = enrichReviewsWithUserInfo(filteredReviews);
                
                // Return paginated response
                Page<Map<String, Object>> enrichedPage = new org.springframework.data.domain.PageImpl<>(
                    enrichedReviews, 
                    pageable, 
                    filteredReviews.size()
                );
                return ResponseEntity.ok(enrichedPage);
            } else {
                // Return all reviews for product (backward compatibility)
                List<ProductReview> reviews = reviewService.getProductReviews(productId);
                List<Map<String, Object>> enrichedReviews = enrichReviewsWithUserInfo(reviews);
                return ResponseEntity.ok(enrichedReviews);
            }
        } catch (Exception e) {
            logger.error("Error getting product reviews for productId: {}", productId, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", e.getMessage() != null ? e.getMessage() : "Failed to fetch reviews"
            ));
        }
    }
    
    /**
     * Get reviews by user with user information
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserReviews(@PathVariable String userId) {
        try {
            List<ProductReview> reviews = reviewService.getUserReviews(userId);
            List<Map<String, Object>> enrichedReviews = enrichReviewsWithUserInfo(reviews);
            return ResponseEntity.ok(enrichedReviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get review by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductReview> getReview(@PathVariable Long id) {
        try {
            ProductReview review = reviewService.getReviewById(id);
            if (review != null) {
                return ResponseEntity.ok(review);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Create new review (Requires Authentication)
     */
    @PostMapping("")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> body, Authentication authentication) {
        try {
            // Extract userId from authentication
            String extractedUserId = AuthUtils.extractUserIdFromAuth(authentication);
            logger.info("Extracted user ID from authentication: {}", extractedUserId);
            
            if (extractedUserId == null || extractedUserId.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot extract user ID from authentication", "message", "Please make sure you are logged in."));
            }
            
            // Resolve user identifier to UUID if needed
            String userId = resolveUserId(extractedUserId);
            logger.info("Resolved user ID: {} -> {}", extractedUserId, userId);
            
            if (userId == null || userId.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot resolve user ID", "message", "Please make sure you are logged in with a valid account."));
            }
            
            // Extract review data from request body
            String productId = body.get("productId").toString();
            Integer rating = Integer.valueOf(body.get("rating").toString());
            String title = (String) body.getOrDefault("title", "");
            String content = (String) body.getOrDefault("content", "");
            String pros = (String) body.getOrDefault("pros", null);
            String cons = (String) body.getOrDefault("cons", null);
            
            // Validation
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rating must be between 1 and 5", "message", "Rating must be between 1 and 5"));
            }
            
            if (title.trim().isEmpty() || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Title and content are required", "message", "Title and content are required"));
            }
            
            // Build content with pros and cons if provided
            StringBuilder fullContent = new StringBuilder(content.trim());
            if (pros != null && !pros.trim().isEmpty()) {
                fullContent.append("\n\nƯu điểm: ").append(pros.trim());
            }
            if (cons != null && !cons.trim().isEmpty()) {
                fullContent.append("\n\nNhược điểm: ").append(cons.trim());
            }
            
            // Create ProductReview object
            ProductReview review = new ProductReview();
            review.setUserId(userId);
            review.setProductId(productId);
            review.setRating(rating);
            review.setTitle(title.trim());
            review.setContent(fullContent.toString());
            
            ProductReview savedReview = reviewService.createReview(review);
            return ResponseEntity.ok(savedReview);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage(), "message", "Failed to create review. Please make sure you are logged in."));
        }
    }
    
    private String resolveUserId(String extractedId) {
        if (extractedId == null || extractedId.isEmpty()) {
            return null;
        }
        
        // If already a UUID, return as-is
        if (SecurityUtils.isValidUUID(extractedId)) {
            logger.debug("User ID is already a UUID: {}", extractedId);
            return extractedId;
        }
        
        // Try to resolve non-UUID identifier (username/email) to UUID via user-service
        try {
            URI profileUri = UriComponentsBuilder
                .fromHttpUrl(userServiceUrl + "/api/users/profile")
                .queryParam("identifier", extractedId)
                .build(true)
                .toUri();
            
            logger.info("Attempting to resolve user identifier '{}' via: {}", extractedId, profileUri);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> userResponse = restTemplate.getForObject(profileUri, Map.class);
            
            if (userResponse != null && userResponse.get("id") != null) {
                String resolvedId = userResponse.get("id").toString();
                logger.info("Successfully resolved user identifier '{}' to UUID '{}'", extractedId, resolvedId);
                return resolvedId;
            } else {
                logger.warn("User service returned null or missing ID for identifier: {}", extractedId);
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            logger.error("User not found via user-service for identifier: {}. Response: {}", extractedId, e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("HTTP error resolving user identifier '{}': {} - {}", extractedId, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Exception resolving user identifier '{}': {}", extractedId, e.getMessage(), e);
        }
        
        // Fallback: return original identifier (will be validated later)
        logger.warn("Could not resolve user identifier '{}', using original value", extractedId);
        return extractedId;
    }
    
    /**
     * Update review (Requires Authentication)
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateReview(@PathVariable Long id, @RequestBody ProductReview review) {
        try {
            review.setId(id);
            ProductReview updatedReview = reviewService.updateReview(review);
            return ResponseEntity.ok(updatedReview);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage(), "message", "Failed to update review"));
        }
    }
    
    /**
     * Delete review (Requires Authentication)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteReview(@PathVariable Long id) {
        try {
            reviewService.deleteReview(id);
            return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage(), "message", "Failed to delete review"));
        }
    }
    
    /**
     * Vote on review helpfulness
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<Map<String, Object>> voteReview(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Boolean isHelpful = (Boolean) request.get("isHelpful");
            Map<String, Object> result = reviewService.voteReview(id, isHelpful);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Mark review as helpful (frontend compatibility endpoint)
     */
    @PostMapping("/{id}/helpful")
    public ResponseEntity<Map<String, Object>> markReviewHelpful(@PathVariable Long id) {
        try {
            Map<String, Object> result = reviewService.voteReview(id, true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get product rating statistics
     */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductRatingStats(@PathVariable String productId) {
        try {
            Map<String, Object> stats = reviewService.getProductRatingStats(productId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get product review summary (frontend compatibility endpoint)
     */
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<Map<String, Object>> getProductReviewSummary(@PathVariable String productId) {
        try {
            Map<String, Object> stats = reviewService.getProductRatingStats(productId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get approved reviews only with user information
     */
    @GetMapping("/product/{productId}/approved")
    public ResponseEntity<List<Map<String, Object>>> getApprovedProductReviews(@PathVariable String productId) {
        try {
            List<ProductReview> reviews = reviewService.getApprovedProductReviews(productId);
            List<Map<String, Object>> enrichedReviews = enrichReviewsWithUserInfo(reviews);
            return ResponseEntity.ok(enrichedReviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Enrich reviews with user information (name, avatar, etc.)
     */
    private List<Map<String, Object>> enrichReviewsWithUserInfo(List<ProductReview> reviews) {
        List<Map<String, Object>> enrichedReviews = new ArrayList<>();
        
        for (ProductReview review : reviews) {
            Map<String, Object> reviewMap = new HashMap<>();
            
            // Copy all review fields
            reviewMap.put("id", review.getId());
            reviewMap.put("productId", review.getProductId());
            reviewMap.put("userId", review.getUserId());
            reviewMap.put("rating", review.getRating());
            reviewMap.put("title", review.getTitle());
            reviewMap.put("content", review.getContent());
            reviewMap.put("verifiedPurchase", review.isVerifiedPurchase());
            reviewMap.put("isApproved", review.isApproved());
            reviewMap.put("helpfulVotes", review.getHelpfulVotes());
            reviewMap.put("totalVotes", review.getTotalVotes());
            reviewMap.put("createdAt", review.getCreatedAt());
            reviewMap.put("updatedAt", review.getUpdatedAt());
            
            // Fetch user information (including avatar) from user-service
            // Wrap in try-catch to prevent failure if user-service is unavailable
            try {
                URI userUri = UriComponentsBuilder
                    .fromHttpUrl(userServiceUrl + "/api/users/profile")
                    .queryParam("identifier", review.getUserId())
                    .build(true)
                    .toUri();
                
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = restTemplate.getForObject(userUri, Map.class);
                
                if (userInfo != null) {
                    String firstName = userInfo.get("firstName") != null ? userInfo.get("firstName").toString() : "";
                    String lastName = userInfo.get("lastName") != null ? userInfo.get("lastName").toString() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    if (fullName.isEmpty()) {
                        fullName = userInfo.get("username") != null ? userInfo.get("username").toString() : "User";
                    }
                    
                    // Get avatar URL (check both avatarUrl and profileImageUrl)
                    Object avatarUrl = userInfo.get("avatarUrl");
                    if (avatarUrl == null) {
                        avatarUrl = userInfo.get("profileImageUrl");
                    }
                    
                    reviewMap.put("userName", fullName);
                    reviewMap.put("userAvatar", avatarUrl != null ? avatarUrl.toString() : null);
                    
                    // Include full user object for frontend
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", userInfo.get("id"));
                    userMap.put("username", userInfo.get("username"));
                    userMap.put("firstName", firstName);
                    userMap.put("lastName", lastName);
                    if (avatarUrl != null) {
                        userMap.put("avatarUrl", avatarUrl.toString());
                        userMap.put("profileImageUrl", avatarUrl.toString());
                    }
                    reviewMap.put("user", userMap);
                } else {
                    // Fallback if user not found
                    reviewMap.put("userName", "User");
                    reviewMap.put("userAvatar", null);
                }
            } catch (Exception e) {
                // Log error but don't fail the entire request
                logger.warn("Failed to fetch user info for userId: {}, error: {}", review.getUserId(), e.getMessage());
                // Set default values
                reviewMap.put("userName", "User");
                reviewMap.put("userAvatar", null);
                // If user service is unavailable, use fallback
                reviewMap.put("userName", "User");
                reviewMap.put("userAvatar", null);
            }
            
            enrichedReviews.add(reviewMap);
        }
        
        return enrichedReviews;
    }
    
    /**
     * Approve review (Admin only)
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveReview(@PathVariable Long id) {
        try {
            reviewService.approveReview(id);
            return ResponseEntity.ok(Map.of("message", "Review approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Reject review (Admin only)
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectReview(@PathVariable Long id) {
        try {
            reviewService.rejectReview(id);
            return ResponseEntity.ok(Map.of("message", "Review rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
