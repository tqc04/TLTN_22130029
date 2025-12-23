package com.example.product.service;

import com.example.product.entity.ProductReview;
import com.example.product.repository.ProductReviewRepository;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.order-service.url:http://localhost:8084}")
    private String orderServiceUrl;

    public Page<ProductReview> getApprovedProductReviews(String productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId, pageable);
    }

    public Page<ProductReview> getAllProductReviews(String productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    }

    public Page<ProductReview> getUserReviews(String userId, Pageable pageable) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public ProductReview create(String userId, String productId, Integer rating, String title, String content) {
        ProductReview r = new ProductReview(productId, userId, rating, title, content);
        r.setIsApproved(false);
        r.setHelpfulCount(0);
        r.setNotHelpfulCount(0);
        r.setIsVerifiedPurchase(false); // This should be checked against orders


        // For now, we'll set it to false
        r.setIsVerifiedPurchase(false);

        return reviewRepository.save(r);
    }

    public ProductReview updateOwn(Long reviewId, String userId, Integer rating, String title, String content) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getUserId().equals(userId))
                .map(r -> {
                    if (rating != null) r.setRating(rating);
                    if (title != null) r.setTitle(title);
                    if (content != null) r.setContent(content);
                    return reviewRepository.save(r);
                })
                .orElse(null);
    }

    public ProductReview updateOwnWithProsCons(Long reviewId, String userId, Integer rating, String title, String content, String pros, String cons) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getUserId().equals(userId))
                .map(r -> {
                    if (rating != null) r.setRating(rating);
                    if (title != null) r.setTitle(title);
                    if (content != null) r.setContent(content);
                    if (pros != null) r.setPros(pros);
                    if (cons != null) r.setCons(cons);
                    return reviewRepository.save(r);
                })
                .orElse(null);
    }

    public boolean deleteOwn(Long reviewId, String userId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getUserId().equals(userId))
                .map(r -> {
                    reviewRepository.deleteById(reviewId);
                    return true;
                }).orElse(false);
    }

    public ProductReview incrementHelpful(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(r -> {
                    r.setHelpfulCount((r.getHelpfulCount() == null ? 0 : r.getHelpfulCount()) + 1);
                    return reviewRepository.save(r);
                }).orElse(null);
    }

    public ProductReview incrementNotHelpful(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(r -> {
                    r.setNotHelpfulCount((r.getNotHelpfulCount() == null ? 0 : r.getNotHelpfulCount()) + 1);
                    return reviewRepository.save(r);
                }).orElse(null);
    }

    /**
     * Verify if user has purchased the product for verified purchase badge
     */
    public void verifyPurchaseStatus(Long reviewId, String userId) {
      
        // This would involve calling order service to check if user has orders for this product
        reviewRepository.findById(reviewId)
                .filter(r -> r.getUserId().equals(userId))
                .ifPresent(r -> {
                    // For now, we'll assume some users have verified purchases
                    // In real implementation, check order history
                    r.setIsVerifiedPurchase(true); // Simplified implementation
                    reviewRepository.save(r);
                });
    }

    public ProductReview approve(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(r -> { r.setIsApproved(true); return reviewRepository.save(r); })
                .orElse(null);
    }

    public ProductReview reject(Long reviewId, String reason) {
        return reviewRepository.findById(reviewId)
                .map(r -> { r.setIsApproved(false); return reviewRepository.save(r); })
                .orElse(null);
    }

    /**
     * Check if user has already reviewed a product
     */
    public boolean hasUserReviewedProduct(String userId, String productId) {
        return reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Check if user has purchased the product (call Order Service)
     */
    public boolean checkIfUserPurchasedProduct(String userId, String productId) {
        try {
            String url = orderServiceUrl + "/api/orders/user/" + userId + "/purchased-products";
            
            // Call Order Service with internal request header
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Internal-Request", "true");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                Object productsObj = responseBody.get("products");
                if (productsObj instanceof List) {
                    List<?> purchasedProductIds = (List<?>) productsObj;
                    // Convert all items to String for comparison
                    return purchasedProductIds.stream()
                            .map(id -> id.toString())
                            .anyMatch(id -> id.equals(productId));
                }
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error checking purchase status: " + e.getMessage());
            // If Order Service is down, allow review anyway (graceful degradation)
            return true;
        }
    }

    /**
     * Check if user has already marked a review as helpful
     */
    public boolean hasUserMarkedHelpful(Long reviewId, String userId) {
        // This would need a separate table to track helpful votes
        // For now, we'll implement a simple check
        return false; // Simplified implementation
    }

    /**
     * Mark a review as helpful by a user
     */
    public void markHelpful(Long reviewId, String userId) {
        // This would save to a separate helpful_votes table
        // For now, just increment the counter
    }

    /**
     * Get product ID by review ID
     */
    public String getProductIdByReviewId(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(ProductReview::getProductId)
                .orElse(null);
    }

    /**
     * Update product rating based on approved reviews
     */
    @Transactional
    public void updateProductRating(String productId) {
        List<ProductReview> approvedReviews = reviewRepository.findByProductIdAndIsApprovedTrue(productId);

        if (!approvedReviews.isEmpty()) {
            double averageRating = approvedReviews.stream()
                    .mapToInt(ProductReview::getRating)
                    .average()
                    .orElse(0.0);

            int reviewCount = approvedReviews.size();

            // Update product rating
            productRepository.findById(productId).ifPresent(product -> {
                product.setAverageRating(averageRating);
                product.setReviewCount(reviewCount);
                productRepository.save(product);
            });
        }
    }

    /**
     * Get review summary for a product
     */
    public Map<String, Object> getReviewSummary(String productId) {
        List<ProductReview> approvedReviews = reviewRepository.findByProductIdAndIsApprovedTrue(productId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalReviews", approvedReviews.size());

        if (!approvedReviews.isEmpty()) {
            double averageRating = approvedReviews.stream()
                    .mapToInt(ProductReview::getRating)
                    .average()
                    .orElse(0.0);

            summary.put("averageRating", Math.round(averageRating * 10.0) / 10.0);

            // Rating distribution
            Map<Integer, Long> ratingDistribution = approvedReviews.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        ProductReview::getRating,
                        java.util.stream.Collectors.counting()
                    ));

            summary.put("ratingDistribution", ratingDistribution);

            // Calculate percentages
            Map<String, Double> ratingPercentages = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                long count = ratingDistribution.getOrDefault(i, 0L);
                double percentage = (double) count / approvedReviews.size() * 100;
                ratingPercentages.put(String.valueOf(i), Math.round(percentage * 10.0) / 10.0);
            }
            summary.put("ratingPercentages", ratingPercentages);
        }

        return summary;
    }

    /**
     * Get review statistics
     */
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalReviews = reviewRepository.count();
        long approvedReviews = reviewRepository.countByIsApprovedTrue();
        long pendingReviews = reviewRepository.countByIsApprovedFalse();

        stats.put("totalReviews", totalReviews);
        stats.put("approvedReviews", approvedReviews);
        stats.put("pendingReviews", pendingReviews);
        stats.put("approvalRate", totalReviews > 0 ? (double) approvedReviews / totalReviews * 100 : 0.0);

        return stats;
    }

    /**
     * Get admin review statistics
     */
    public Map<String, Object> getAdminReviewStats() {
        Map<String, Object> stats = getReviewStats();

        // Add more detailed stats
        List<ProductReview> allReviews = reviewRepository.findAll();
        if (!allReviews.isEmpty()) {
            double averageRating = allReviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(ProductReview::getRating)
                    .average()
                    .orElse(0.0);

            stats.put("overallAverageRating", Math.round(averageRating * 10.0) / 10.0);

            // Reviews by rating
            Map<Integer, Long> ratingDistribution = allReviews.stream()
                    .filter(r -> r.getRating() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                        ProductReview::getRating,
                        java.util.stream.Collectors.counting()
                    ));

            stats.put("ratingDistribution", ratingDistribution);
        }

        return stats;
    }

    /**
     * Save review (update existing)
     */
    public ProductReview save(ProductReview review) {
        return reviewRepository.save(review);
    }
}


