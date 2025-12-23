package com.example.product.dto;

import com.example.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductDTO {
    private String id;
    private String name;
    private String description;
    private String aiGeneratedDescription;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Double weight;
    private String dimensions;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isDigital;
    private Boolean requiresShipping;
    private Boolean isOnSale;
    private Double averageRating;
    private Integer reviewCount;
    private Long viewCount;
    private Long purchaseCount;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
    private String imageUrl;
    private String seoTitle;
    private String seoDescription;
    private String tags;
    private String category;
    private String brand;
    private Long categoryId;
    private Long brandId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductVariantDTO> variants;
    private List<Map<String, Object>> images;

    public static ProductDTO from(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.id = product.getId();
        dto.name = product.getName();
        dto.description = product.getDescription();
        dto.aiGeneratedDescription = product.getAiGeneratedDescription();
        dto.sku = product.getSku();
        dto.price = product.getPrice();
        dto.salePrice = product.getSalePrice();
        dto.compareAtPrice = product.getCompareAtPrice();
        dto.costPrice = product.getCostPrice();
        dto.stockQuantity = product.getStockQuantity();
        dto.lowStockThreshold = product.getLowStockThreshold();
        dto.weight = product.getWeight();
        dto.dimensions = product.getDimensions();
        dto.isActive = product.getIsActive();
        dto.isFeatured = product.getIsFeatured();
        dto.isDigital = product.getIsDigital();
        dto.requiresShipping = product.getRequiresShipping();
        dto.isOnSale = product.getIsOnSale();
        dto.averageRating = product.getAverageRating();
        dto.reviewCount = product.getReviewCount();
        dto.viewCount = product.getViewCount();
        dto.purchaseCount = product.getPurchaseCount();
        dto.saleStartAt = product.getSaleStartAt();
        dto.saleEndAt = product.getSaleEndAt();
        dto.imageUrl = transformImageUrl(product.getImageUrl());
        dto.seoTitle = product.getSeoTitle();
        dto.seoDescription = product.getSeoDescription();
        dto.tags = product.getTags();
        dto.categoryId = product.getCategoryId();
        dto.brandId = product.getBrandId();
        dto.createdAt = product.getCreatedAt();
        dto.updatedAt = product.getUpdatedAt();

        // Map variants
        if (product.getVariants() != null) {
            dto.variants = product.getVariants().stream()
                .map(ProductVariantDTO::from)
                .collect(Collectors.toList());
        }

        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAiGeneratedDescription() { return aiGeneratedDescription; }
    public void setAiGeneratedDescription(String aiGeneratedDescription) { this.aiGeneratedDescription = aiGeneratedDescription; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
    public Boolean getIsDigital() { return isDigital; }
    public void setIsDigital(Boolean isDigital) { this.isDigital = isDigital; }
    public Boolean getRequiresShipping() { return requiresShipping; }
    public void setRequiresShipping(Boolean requiresShipping) { this.requiresShipping = requiresShipping; }
    public Boolean getIsOnSale() { return isOnSale; }
    public void setIsOnSale(Boolean isOnSale) { this.isOnSale = isOnSale; }
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ProductVariantDTO> getVariants() { return variants; }
    public void setVariants(List<ProductVariantDTO> variants) { this.variants = variants; }
    public List<Map<String, Object>> getImages() { return images; }
    public void setImages(List<Map<String, Object>> images) { this.images = images; }
    
    /**
     * Transform image URL from database to full URL for frontend
     * - If null or empty, return as is
     * - If starts with http:// or https://, return as is (external/cloud URL)
     * - If starts with /uploads/, prepend /api to make it accessible via gateway
     * - Otherwise return as is
     */
    public static String transformImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return imageUrl;
        }
        
        String trimmed = imageUrl.trim();
        
        // Already a full URL (external/cloud)
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        
        // Local path starting with /uploads/ - prepend /api for gateway routing
        if (trimmed.startsWith("/uploads/")) {
            return "/api" + trimmed;
        }
        
        // Other cases - return as is
        return trimmed;
    }
}


