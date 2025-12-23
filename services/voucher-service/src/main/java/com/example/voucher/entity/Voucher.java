package com.example.voucher.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@EntityListeners(AuditingEntityListener.class)
public class Voucher {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private com.example.voucher.entity.VoucherType type;
    
    @Column(nullable = false)
    private BigDecimal value;
    
    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;
    
    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "usage_limit")
    private Integer usageLimit;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
    @Column(name = "usage_limit_per_user")
    private Integer usageLimitPerUser;
    
    @Column(name = "is_active")
    private boolean isActive = true;
    
    @Column(name = "is_public")
    private boolean isPublic = true;
    
    @Column(name = "applicable_to")
    private String applicableTo; // ALL, CATEGORY, BRAND, PRODUCT
    
    @Column(name = "applicable_items", columnDefinition = "TEXT")
    private String applicableItems; // JSON array of IDs
    
    @Column(name = "free_shipping", nullable = false)
    private Boolean freeShipping = false;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Voucher() {}
    
    public Voucher(String code, String name, VoucherType type, BigDecimal value) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.value = value;
    }
    
    // Business methods
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               (startDate == null || !now.isBefore(startDate)) &&
               (endDate == null || now.isBefore(endDate)) &&
               (usageLimit == null || usageCount < usageLimit);
    }
    
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid() || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        if (type == com.example.voucher.entity.VoucherType.PERCENTAGE) {
            discount = orderAmount.multiply(value).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else if (type == com.example.voucher.entity.VoucherType.FIXED_AMOUNT) {
            discount = value;
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
        }
        
        return discount;
    }
    
    public void incrementUsage() {
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public VoucherType getType() {
        return type;
    }
    
    public void setType(VoucherType type) {
        this.type = type;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    
    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }
    
    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }
    
    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }
    
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public Integer getUsageLimit() {
        return usageLimit;
    }
    
    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }
    
    public Integer getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
    
    public Integer getUsageLimitPerUser() {
        return usageLimitPerUser;
    }
    
    public void setUsageLimitPerUser(Integer usageLimitPerUser) {
        this.usageLimitPerUser = usageLimitPerUser;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }
    
    public String getApplicableTo() {
        return applicableTo;
    }
    
    public void setApplicableTo(String applicableTo) {
        this.applicableTo = applicableTo;
    }
    
    public String getApplicableItems() {
        return applicableItems;
    }
    
    public void setApplicableItems(String applicableItems) {
        this.applicableItems = applicableItems;
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
    
    public Boolean getFreeShipping() {
        return freeShipping;
    }
    
    public void setFreeShipping(Boolean freeShipping) {
        this.freeShipping = freeShipping;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
}
