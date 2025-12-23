package com.example.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_reviews", indexes = {
    @Index(name = "idx_review_product", columnList = "product_id"),
    @Index(name = "idx_review_user", columnList = "user_id"),
    @Index(name = "idx_review_approved", columnList = "is_approved"),
    @Index(name = "idx_review_rating", columnList = "rating"),
    @Index(name = "idx_review_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @NotNull
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @NotNull
    @Min(1)
    @Max(5)
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @NotBlank
    @Size(min = 5, max = 200)
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "pros", columnDefinition = "TEXT")
    private String pros;

    @Column(name = "cons", columnDefinition = "TEXT")
    private String cons;

    @Column(name = "is_approved")
    private Boolean isApproved = false;

    @Column(name = "is_verified_purchase")
    private Boolean isVerifiedPurchase = false;

    @Column(name = "helpful_count")
    private Integer helpfulCount = 0;

    @Column(name = "not_helpful_count")
    private Integer notHelpfulCount = 0;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ProductReview() {}

    public ProductReview(String productId, String userId, Integer rating, String title, String content) {
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPros() { return pros; }
    public void setPros(String pros) { this.pros = pros; }

    public String getCons() { return cons; }
    public void setCons(String cons) { this.cons = cons; }

    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean approved) { isApproved = approved; }

    public Boolean getIsVerifiedPurchase() { return isVerifiedPurchase; }
    public void setIsVerifiedPurchase(Boolean verifiedPurchase) { isVerifiedPurchase = verifiedPurchase; }

    public Integer getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(Integer helpfulCount) { this.helpfulCount = helpfulCount; }

    public Integer getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(Integer notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


