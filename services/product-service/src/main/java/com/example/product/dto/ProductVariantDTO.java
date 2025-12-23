package com.example.product.dto;

import com.example.product.entity.ProductVariant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductVariantDTO {
    private Long id;
    private String productId;
    private String variantName;
    private String sku;
    private String barcode;
    private String size;
    private String color;
    private String material;
    private String style;
    private String additionalAttributes;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private Double weightKg;
    private String dimensions;
    private Boolean isActive;
    private Boolean isDefault;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductVariantDTO from(ProductVariant variant) {
        ProductVariantDTO dto = new ProductVariantDTO();
        dto.id = variant.getId();
        dto.productId = variant.getProduct().getId();
        dto.variantName = variant.getVariantName();
        dto.sku = variant.getSku();
        dto.barcode = variant.getBarcode();
        dto.size = variant.getSize();
        dto.color = variant.getColor();
        dto.material = variant.getMaterial();
        dto.style = variant.getStyle();
        dto.additionalAttributes = variant.getAdditionalAttributes();
        dto.price = variant.getPrice();
        dto.costPrice = variant.getCostPrice();
        dto.stockQuantity = variant.getStockQuantity();
        dto.weightKg = variant.getWeightKg();
        dto.dimensions = variant.getDimensions();
        dto.isActive = variant.isActive();
        dto.isDefault = variant.isDefault();
        dto.imageUrl = variant.getImageUrl();
        dto.createdAt = variant.getCreatedAt();
        dto.updatedAt = variant.getUpdatedAt();
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getAdditionalAttributes() { return additionalAttributes; }
    public void setAdditionalAttributes(String additionalAttributes) { this.additionalAttributes = additionalAttributes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public Boolean isActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
    public Boolean isDefault() { return isDefault; }
    public void setDefault(Boolean aDefault) { isDefault = aDefault; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
