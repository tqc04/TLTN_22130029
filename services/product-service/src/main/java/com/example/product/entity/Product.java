package com.example.product.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_brand", columnList = "brand_id"),
    @Index(name = "idx_product_price", columnList = "price"),
    @Index(name = "idx_product_active", columnList = "is_active"),
    @Index(name = "idx_product_sku", columnList = "sku")
})
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotBlank
    @Size(min = 3, max = 200)
    private String name;
    
    @NotBlank
    @Size(min = 10, max = 2000)
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    
    @Column(name = "ai_generated_description", columnDefinition = "LONGTEXT")
    private String aiGeneratedDescription;
    
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true)
    private String sku;
    
    @NotNull
    @DecimalMin(value = "0.01")
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    private BigDecimal compareAtPrice;
    
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;
    
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;
    
    @Column(name = "weight")
    private Double weight;
    
    @Column(name = "dimensions")
    private String dimensions;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;
    
    @Column(name = "is_digital")
    private Boolean isDigital = false;
    
    @Column(name = "requires_shipping")
    private Boolean requiresShipping = true;

    @Column(name = "is_on_sale")
    private Boolean isOnSale = false;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "purchase_count")
    private Long purchaseCount;

    @Column(name = "sale_start_at")
    private LocalDateTime saleStartAt;

    @Column(name = "sale_end_at")
    private LocalDateTime saleEndAt;

    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "seo_title")
    private String seoTitle;
    
    @Column(name = "seo_description")
    private String seoDescription;
    
    @Column(name = "tags", columnDefinition = "LONGTEXT")
    private String tags;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "brand_id")
    private Long brandId;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private java.util.List<ProductVariant> variants = new java.util.ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Product() {}

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
    public Boolean getIsDigital() { return isDigital; }
    public void setIsDigital(Boolean isDigital) { this.isDigital = isDigital; }
    public Boolean getRequiresShipping() { return requiresShipping; }
    public void setRequiresShipping(Boolean requiresShipping) { this.requiresShipping = requiresShipping; }
    public Boolean getIsOnSale() { return isOnSale; }
    public void setIsOnSale(Boolean isOnSale) { this.isOnSale = isOnSale; }
    public String getAiGeneratedDescription() { return aiGeneratedDescription; }
    public void setAiGeneratedDescription(String aiGeneratedDescription) { this.aiGeneratedDescription = aiGeneratedDescription; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public Long getPurchaseCount() { return purchaseCount; }
    public void setPurchaseCount(Long purchaseCount) { this.purchaseCount = purchaseCount; }
    public LocalDateTime getSaleStartAt() { return saleStartAt; }
    public void setSaleStartAt(LocalDateTime saleStartAt) { this.saleStartAt = saleStartAt; }
    public LocalDateTime getSaleEndAt() { return saleEndAt; }
    public void setSaleEndAt(LocalDateTime saleEndAt) { this.saleEndAt = saleEndAt; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public java.util.List<ProductVariant> getVariants() { return variants; }
    public void setVariants(java.util.List<ProductVariant> variants) { this.variants = variants; }
}


