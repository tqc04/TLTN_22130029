package com.example.voucher.dto;

import com.example.voucher.entity.Voucher;
import com.example.voucher.entity.VoucherType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private VoucherType type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usageCount;
    private Integer usageLimitPerUser;
    private boolean isActive;
    private boolean isPublic;
    private String applicableTo;
    private String applicableItems;
    private Boolean freeShipping;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VoucherDTO from(Voucher voucher) {
        VoucherDTO dto = new VoucherDTO();
        dto.id = voucher.getId();
        dto.code = voucher.getCode();
        dto.name = voucher.getName();
        dto.description = voucher.getDescription();
        dto.type = voucher.getType();
        dto.value = voucher.getValue();
        dto.minOrderAmount = voucher.getMinOrderAmount();
        dto.maxDiscountAmount = voucher.getMaxDiscountAmount();
        dto.startDate = voucher.getStartDate();
        dto.endDate = voucher.getEndDate();
        dto.usageLimit = voucher.getUsageLimit();
        dto.usageCount = voucher.getUsageCount();
        dto.usageLimitPerUser = voucher.getUsageLimitPerUser();
        dto.isActive = voucher.isActive();
        dto.isPublic = voucher.isPublic();
        dto.applicableTo = voucher.getApplicableTo();
        dto.applicableItems = voucher.getApplicableItems();
        dto.freeShipping = voucher.getFreeShipping();
        dto.imageUrl = voucher.getImageUrl();
        dto.createdAt = voucher.getCreatedAt();
        dto.updatedAt = voucher.getUpdatedAt();
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public VoucherType getType() { return type; }
    public void setType(VoucherType type) { this.type = type; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public Integer getUsageLimitPerUser() { return usageLimitPerUser; }
    public void setUsageLimitPerUser(Integer usageLimitPerUser) { this.usageLimitPerUser = usageLimitPerUser; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public String getApplicableTo() { return applicableTo; }
    public void setApplicableTo(String applicableTo) { this.applicableTo = applicableTo; }

    public String getApplicableItems() { return applicableItems; }
    public void setApplicableItems(String applicableItems) { this.applicableItems = applicableItems; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getFreeShipping() { return freeShipping; }
    public void setFreeShipping(Boolean freeShipping) { this.freeShipping = freeShipping; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
