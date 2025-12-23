package com.example.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@EntityListeners(AuditingEntityListener.class)
public class ProductVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;
    
    @NotNull
    @Column(name = "variant_name")
    private String variantName;
    
    @Column(name = "sku", unique = true)
    private String sku;
    
    @Column(name = "barcode")
    private String barcode;
    
    @Column(name = "size")
    private String size;
    
    @Column(name = "color")
    private String color;
    
    @Column(name = "material")
    private String material;
    
    @Column(name = "style")
    private String style;
    
    @Column(name = "additional_attributes", columnDefinition = "TEXT")
    private String additionalAttributes; // JSON string
    
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;
    
    @DecimalMin(value = "0.0")
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;
    
    @Min(0)
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;
    
    @Column(name = "weight_kg")
    private Double weightKg;
    
    @Column(name = "dimensions")
    private String dimensions;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ProductVariant() {}
    
    public ProductVariant(Product product, String variantName, String sku) {
        this.product = product;
        this.variantName = variantName;
        this.sku = sku;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public String getVariantName() {
        return variantName;
    }
    
    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }
    
    public String getSku() {
        return sku;
    }
    
    public void setSku(String sku) {
        this.sku = sku;
    }
    
    public String getBarcode() {
        return barcode;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getMaterial() {
        return material;
    }
    
    public void setMaterial(String material) {
        this.material = material;
    }
    
    public String getStyle() {
        return style;
    }
    
    public void setStyle(String style) {
        this.style = style;
    }
    
    public String getAdditionalAttributes() {
        return additionalAttributes;
    }
    
    public void setAdditionalAttributes(String additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getCostPrice() {
        return costPrice;
    }
    
    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
    
    public Integer getStockQuantity() {
        return stockQuantity;
    }
    
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    
    public Double getWeightKg() {
        return weightKg;
    }
    
    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public Boolean isActive() {
        return isActive;
    }
    
    public void setActive(Boolean active) {
        isActive = active;
    }
    
    public Boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
