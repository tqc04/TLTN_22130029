package com.example.review.repository;

import com.example.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<ProductReview, Long> {
    
    /**
     * Find reviews by product ID and approved status
     */
    List<ProductReview> findByProductIdAndIsApprovedTrue(String productId);
    
    /**
     * Find reviews by user ID
     */
    List<ProductReview> findByUserId(String userId);
    
    /**
     * Find reviews by product ID (all statuses)
     */
    List<ProductReview> findByProductId(String productId);
    
    /**
     * Find reviews by rating
     */
    List<ProductReview> findByRating(Integer rating);
    
    /**
     * Find reviews by verified purchase status
     */
    List<ProductReview> findByVerifiedPurchaseTrue();
    
    /**
     * Find reviews by spam status
     */
    List<ProductReview> findByIsSpamTrue();
    
    /**
     * Find pending reviews (not approved)
     */
    List<ProductReview> findByIsApprovedFalse();
    
    /**
     * Find reviews by product and user
     */
    List<ProductReview> findByProductIdAndUserId(String productId, String userId);  
    
    /**
     * Count reviews by product
     */
    long countByProductId(String productId);
    
    /**
     * Count reviews by product and approved status
     */
    long countByProductIdAndIsApprovedTrue(String productId);
    
    /**
     * Count reviews by user
     */
    long countByUserId(String userId);
    
    /**
     * Get average rating for product
     */
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId AND r.isApproved = true")
    Double getAverageRatingByProductId(@Param("productId") String productId); 
    
    /**
     * Get reviews with pagination by product
     */
    Page<ProductReview> findByProductIdAndIsApprovedTrue(String productId, Pageable pageable);
    
    /**
     * Get reviews with pagination by user
     */
    Page<ProductReview> findByUserId(String userId, Pageable pageable);
    
    /**
     * Get reviews with pagination by rating
     */
    Page<ProductReview> findByRatingAndIsApprovedTrue(Integer rating, Pageable pageable);
    
    /**
     * Get top helpful reviews
     */
    @Query("SELECT r FROM ProductReview r WHERE r.isApproved = true ORDER BY (r.helpfulVotes * 1.0 / NULLIF(r.totalVotes, 0)) DESC")
    List<ProductReview> findTopHelpfulReviews(Pageable pageable);
    
    /**
     * Get recent reviews
     */
    @Query("SELECT r FROM ProductReview r WHERE r.isApproved = true ORDER BY r.createdAt DESC")
    List<ProductReview> findRecentReviews(Pageable pageable);
    
    /**
     * Get reviews by sentiment
     */
    @Query("SELECT r FROM ProductReview r WHERE r.sentimentLabel = :sentiment AND r.isApproved = true")
    List<ProductReview> findBySentimentLabel(@Param("sentiment") String sentiment);
    
    /**
     * Get reviews with high spam score
     */
    @Query("SELECT r FROM ProductReview r WHERE r.spamScore > :threshold")
    List<ProductReview> findBySpamScoreGreaterThan(@Param("threshold") Double threshold);
    
    /**
     * Check if review exists by product and user
     */
    boolean existsByProductIdAndUserId(String productId, String userId);
}
