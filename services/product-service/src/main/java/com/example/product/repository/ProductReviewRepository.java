package com.example.product.repository;

import com.example.product.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Page<ProductReview> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(String productId, Pageable pageable);
    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    Page<ProductReview> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Check if user has reviewed a product
    boolean existsByUserIdAndProductId(String userId, String productId);

    // Count approved reviews
    long countByIsApprovedTrue();

    // Count pending reviews
    long countByIsApprovedFalse();

    // Get approved reviews for a product (for rating calculation)111
    java.util.List<ProductReview> findByProductIdAndIsApprovedTrue(String productId);
}


