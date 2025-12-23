package com.example.review.service;

import com.example.review.entity.ProductReview;
import com.example.review.repository.ReviewRepository;
import com.example.shared.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.product.base-url:http://product-service}")
    private String productServiceUrl;
    
    @Value("${services.user.base-url:http://user-service}")
    private String userServiceUrl;
    
    @Value("${services.ai.base-url:http://ai-service}")
    private String aiServiceUrl;
    
    @Value("${services.order.base-url:http://order-service}")
    private String orderServiceUrl;
    
    /**
     * Get all reviews with pagination
     */
    public Page<ProductReview> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }
    
    /**
     * Get reviews for a specific product
     */
    public List<ProductReview> getProductReviews(String productId) {
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId);
    }
    
    /**
     * Get reviews by user
     */
    public List<ProductReview> getUserReviews(String userId) {
        return reviewRepository.findByUserId(userId);
    }
    
    /**
     * Get review by ID
     */
    public ProductReview getReviewById(Long id) {
        Optional<ProductReview> review = reviewRepository.findById(id);
        return review.orElse(null);
    }
    
    /**
     * Create new review - Check if user purchased the product
     * User is already authenticated via JWT, so we trust the userId from authentication
     */
    public ProductReview createReview(ProductReview review) {
        // Validate product exists
        validateProductExists(review.getProductId());
        
        // Resolve and normalize user ID to UUID if possible (but don't fail if not found)
        // Since user is already authenticated via JWT, we trust the userId from authentication
        String normalizedUserId = resolveUserId(review.getUserId());
        review.setUserId(normalizedUserId);
        
        // NOTE: We don't validate user exists because:
        // 1. User is already authenticated via JWT (Spring Security validates this)
        // 2. We trust the userId from the authenticated JWT token
        // 3. If user-service is down or user not found, we still allow review creation
        
        // Check if user already reviewed this product - if yes, update existing review
        List<ProductReview> existingReviews = reviewRepository.findByProductIdAndUserId(review.getProductId(), normalizedUserId);
        ProductReview savedReview;
        
        if (!existingReviews.isEmpty()) {
            // Update existing review
            ProductReview existingReview = existingReviews.get(0);
            existingReview.setRating(review.getRating());
            existingReview.setTitle(review.getTitle());
            existingReview.setContent(review.getContent());
            // Auto-approve updated review so it's visible immediately
            existingReview.setIsApproved(true);
            // Keep existing helpful votes and total votes
            savedReview = reviewRepository.save(existingReview);
            logger.info("Review updated for product {} by user {} (review ID: {})", 
                review.getProductId(), normalizedUserId, existingReview.getId());
        } else {
            // Create new review
            // Check if user has purchased the product (but don't block if check fails)
            boolean hasPurchased = hasUserPurchasedProduct(normalizedUserId, review.getProductId());
            
            // Set default values
            // Auto-approve review so it's visible immediately to other customers
            // AI analysis will still run to detect spam, and admin can reject later if needed
            review.setIsApproved(true);
            review.setVerifiedPurchase(hasPurchased); // Set based on purchase check result
            review.setHelpfulVotes(0);
            review.setTotalVotes(0);
            review.setIsSpam(false);
            
            // Save review
            savedReview = reviewRepository.save(review);
            logger.info("Review created for product {} by user {} (verified purchase: {})", 
                review.getProductId(), review.getUserId(), hasPurchased);
        }
        
        // Perform AI analysis asynchronously (don't block the review creation)
        try {
            performAIAnalysis(savedReview);
        } catch (Exception e) {
            logger.error("AI analysis failed but review was created: {}", e.getMessage(), e);
        }
        
        return savedReview;
    }
    
    /**
     * Update review
     */
    public ProductReview updateReview(ProductReview review) {
        // Validate review exists
        if (!reviewRepository.existsById(review.getId())) {
            throw new RuntimeException("Review not found");
        }
        
        // Re-analyze with AI after update
        performAIAnalysis(review);
        
        return reviewRepository.save(review);
    }
    
    /**
     * Delete review
     */
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Review not found");
        }
        reviewRepository.deleteById(id);
    }
    
    /**
     * Vote on review helpfulness
     */
    public Map<String, Object> voteReview(Long reviewId, Boolean isHelpful) {
        Optional<ProductReview> reviewOpt = reviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found");
        }
        
        ProductReview review = reviewOpt.get();
        review.setTotalVotes(review.getTotalVotes() + 1);
        
        if (isHelpful) {
            review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        }
        
        reviewRepository.save(review);
        
        return Map.of(
            "helpfulVotes", review.getHelpfulVotes(),
            "totalVotes", review.getTotalVotes(),
            "helpfulnessRatio", (double) review.getHelpfulVotes() / review.getTotalVotes()
        );
    }
    
    /**
     * Get all reviews for admin with filters
     */
    public Page<ProductReview> getAllReviewsForAdmin(
            String productId, 
            String userId, 
            Boolean isApproved, 
            Boolean isSpam, 
            Integer rating,
            Pageable pageable) {
        
        // Get all reviews first, then filter
        Page<ProductReview> allReviews = reviewRepository.findAll(pageable);
        
        // Apply filters
        List<ProductReview> filtered = allReviews.getContent().stream()
            .filter(r -> {
                if (productId != null && !productId.isEmpty() && !r.getProductId().equals(productId)) {
                    return false;
                }
                if (userId != null && !userId.isEmpty() && !r.getUserId().equals(userId)) {
                    return false;
                }
                if (isApproved != null && (r.getIsApproved() == null || !r.getIsApproved().equals(isApproved))) {
                    return false;
                }
                if (isSpam != null && (r.getIsSpam() == null || !r.getIsSpam().equals(isSpam))) {
                    return false;
                }
                if (rating != null && (r.getRating() == null || !r.getRating().equals(rating))) {
                    return false;
                }
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Apply pagination to filtered results
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<ProductReview> pagedContent = start < filtered.size() 
            ? filtered.subList(start, end) 
            : new java.util.ArrayList<>();
        
        return new org.springframework.data.domain.PageImpl<>(
            pagedContent, 
            pageable, 
            filtered.size()
        );
    }
    
    /**
     * Get admin review statistics
     */
    public Map<String, Object> getAdminReviewStats() {
        List<ProductReview> allReviews = reviewRepository.findAll();
        
        long totalReviews = allReviews.size();
        long approvedReviews = allReviews.stream().filter(r -> r.getIsApproved() != null && r.getIsApproved()).count();
        long pendingReviews = allReviews.stream().filter(r -> r.getIsApproved() == null || !r.getIsApproved()).count();
        long spamReviews = allReviews.stream().filter(r -> r.getIsSpam() != null && r.getIsSpam()).count();
        
        double averageRating = allReviews.stream()
            .filter(r -> r.getRating() != null)
            .mapToInt(ProductReview::getRating)
            .average()
            .orElse(0.0);
        
        // Rating distribution
        Map<Integer, Long> ratingDistribution = allReviews.stream()
            .filter(r -> r.getRating() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                ProductReview::getRating,
                java.util.stream.Collectors.counting()
            ));
        
        // Reviews by status
        Map<String, Long> statusDistribution = Map.of(
            "approved", approvedReviews,
            "pending", pendingReviews,
            "spam", spamReviews
        );
        
        return Map.of(
            "totalReviews", totalReviews,
            "approvedReviews", approvedReviews,
            "pendingReviews", pendingReviews,
            "spamReviews", spamReviews,
            "averageRating", Math.round(averageRating * 10.0) / 10.0,
            "ratingDistribution", ratingDistribution,
            "statusDistribution", statusDistribution,
            "approvalRate", totalReviews > 0 ? Math.round((double) approvedReviews / totalReviews * 100.0) : 0.0
        );
    }
    
    /**
     * Get product rating statistics
     */
    public Map<String, Object> getProductRatingStats(String productId) {
        List<ProductReview> reviews = reviewRepository.findByProductIdAndIsApprovedTrue(productId);
        
        if (reviews.isEmpty()) {
            return Map.of(
                "averageRating", 0.0,
                "totalReviews", 0,
                "ratingDistribution", Map.of()
            );
        }
        
        double averageRating = reviews.stream()
            .mapToInt(ProductReview::getRating)
            .average()
            .orElse(0.0);
        
        Map<String, Long> ratingDistribution = reviews.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                r -> r.getRating().toString(),
                java.util.stream.Collectors.counting()
            ));
        
        return Map.of(
            "averageRating", Math.round(averageRating * 10.0) / 10.0,
            "totalReviews", reviews.size(),
            "ratingDistribution", ratingDistribution
        );
    }
    
    /**
     * Get approved reviews only
     */
    public List<ProductReview> getApprovedProductReviews(String productId) {
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId);
    }
    
    /**
     * Approve review (Admin only)
     */
    public void approveReview(Long id) {
        Optional<ProductReview> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found");
        }
        
        ProductReview review = reviewOpt.get();
        review.setIsApproved(true);
        reviewRepository.save(review);
    }
    
    /**
     * Reject review (Admin only)
     */
    public void rejectReview(Long id) {
        Optional<ProductReview> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isEmpty()) {
            throw new RuntimeException("Review not found");
        }
        
        ProductReview review = reviewOpt.get();
        review.setIsApproved(false);
        reviewRepository.save(review);
    }
    
    /**
     * Validate product exists
     */
    private void validateProductExists(String productId) {
        try {
            restTemplate.getForObject(productServiceUrl + "/api/products/" + productId, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Product not found: " + productId);
        }
    }
    
    /**
     * Validate user exists
     */
    private void validateUserExists(String userId) {
        try {
            URI uri = UriComponentsBuilder
                .fromHttpUrl(userServiceUrl + "/api/users/profile")
                .queryParam("identifier", userId)
                .build(true)
                .toUri();
            
            logger.debug("Validating user existence: {}", uri);
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = restTemplate.getForObject(uri, Map.class);
            
            if (userInfo == null || userInfo.get("id") == null) {
                logger.error("User validation returned null or missing ID for identifier: {}", userId);
                throw new RuntimeException("User not found: " + userId);
            }
            
            logger.debug("User validated successfully: {} -> {}", userId, userInfo.get("id"));
        } catch (HttpClientErrorException.NotFound e) {
            logger.error("User not found via user-service for identifier: {}", userId);
            throw new RuntimeException("User not found: " + userId + ". Please make sure you are logged in with a valid account.");
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error validating user {}: {} - {}", userId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to validate user: " + userId + ". User service returned: " + e.getStatusCode());
        } catch (Exception e) {
            logger.error("Failed to validate user {} via user-service: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to validate user: " + userId + ". Error: " + e.getMessage());
        }
    }

    /**
     * Resolve provided user identifier (username/email) to canonical UUID
     * Returns the original identifier if resolution fails
     * Since user is already authenticated via JWT, we trust the identifier from authentication
     */
    private String resolveUserId(String suppliedId) {
        if (suppliedId == null || suppliedId.isBlank()) {
            throw new RuntimeException("User ID is required");
        }

        // If already a UUID, return as-is
        if (SecurityUtils.isValidUUID(suppliedId)) {
            logger.debug("User ID is already a UUID: {}", suppliedId);
            return suppliedId;
        }

        // Try to resolve non-UUID identifier (username/email) to UUID via user-service
        // This is optional - if it fails, we use the original identifier
        try {
            URI uri = UriComponentsBuilder
                .fromHttpUrl(userServiceUrl + "/api/users/profile")
                .queryParam("identifier", suppliedId)
                .build(true)
                .toUri();

            logger.debug("Attempting to resolve user identifier '{}' to UUID via: {}", suppliedId, uri);
            @SuppressWarnings("unchecked")
            Map<String, Object> userResponse = restTemplate.getForObject(uri, Map.class);
            if (userResponse != null && userResponse.get("id") != null) {
                String resolvedId = userResponse.get("id").toString();
                logger.info("Successfully resolved user identifier '{}' to UUID '{}'", suppliedId, resolvedId);
                return resolvedId;
            } else {
                logger.warn("User service returned null or missing ID for identifier: {}", suppliedId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            // User not found via profile endpoint - use original identifier
            logger.debug("User identifier '{}' not found via profile endpoint, using original identifier", suppliedId);
        } catch (Exception e) {
            logger.warn("Unable to resolve user identifier '{}': {}. Using original identifier.", 
                suppliedId, e.getMessage());
        }

        // Fallback: return original identifier (user is already authenticated via JWT)
        logger.debug("Using original user identifier: {}", suppliedId);
        return suppliedId;
    }
    
    /**
     * Check if user has purchased the product
     */
    private boolean hasUserPurchasedProduct(String userId, String productId) {
        try {
            // Call Order Service to check if user has purchased this product
            String url = orderServiceUrl + "/api/orders/user/" + userId + "/purchased-products";
            System.out.println("Checking purchase history: " + url);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("products")) {
                @SuppressWarnings("unchecked")
                List<String> purchasedProductIds = (List<String>) response.get("products");
                boolean hasPurchased = purchasedProductIds.contains(productId);
                System.out.println("User " + userId + " purchased product " + productId + ": " + hasPurchased);
                return hasPurchased;
            }
            
            System.out.println("No purchase history found for user " + userId);
            return false;
        } catch (Exception e) {
            // Log error but allow review with unverified status
            System.err.println("Failed to check purchase history for user " + userId + 
                ", product " + productId + ": " + e.getMessage());
            // Return false - review will be created but marked as unverified
            return false;
        }
    }
    
    /**
     * Perform AI analysis on review
     */
    private void performAIAnalysis(ProductReview review) {
        try {
            // Call AI service for sentiment analysis (correct endpoint)
            Map<String, Object> aiRequest = Map.of(
                "text", review.getContent()
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> aiResponse = (Map<String, Object>) restTemplate.postForObject(
                aiServiceUrl + "/api/ai/sentiment/analyze", 
                aiRequest, 
                Map.class
            );
            
            if (aiResponse != null && aiResponse.containsKey("sentiment")) {
                // Map AI response to review fields
                String sentiment = (String) aiResponse.get("sentiment");
                Double confidence = aiResponse.get("confidence") != null ? 
                    ((Number) aiResponse.get("confidence")).doubleValue() : 0.0;
                
                // Convert sentiment to score (-1.0 to 1.0)
                double sentimentScore = 0.0;
                if ("POSITIVE".equalsIgnoreCase(sentiment)) {
                    sentimentScore = confidence;
                } else if ("NEGATIVE".equalsIgnoreCase(sentiment)) {
                    sentimentScore = -confidence;
                }
                
                review.setSentimentScore(sentimentScore);
                review.setSentimentLabel(sentiment);
                review.setAiSummary((String) aiResponse.get("explanation"));
                
                // Check for spam indicators (negative with high confidence)
                boolean isNegative = aiResponse.get("isNegative") != null ? 
                    (Boolean) aiResponse.get("isNegative") : false;
                review.setSpamScore(isNegative && confidence > 0.8 ? confidence : 0.0);
                review.setIsSpam(isNegative && confidence > 0.9);
                
                // Save updated review
                reviewRepository.save(review);
                
                System.out.println("AI analysis completed for review " + review.getId() + 
                    ": sentiment=" + sentiment + ", confidence=" + confidence);
            }
        } catch (Exception e) {
            // Log error but don't fail review creation
            System.err.println("AI analysis failed for review " + review.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
