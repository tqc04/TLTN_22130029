package com.example.voucher.dto;

import com.example.voucher.entity.VoucherUsage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VoucherUsageDTO {
    private Long id;
    private Long voucherId;
    private String voucherCode;
    private String userId;
    private Long orderId;
    private String orderNumber;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    public static VoucherUsageDTO from(VoucherUsage usage) {
        VoucherUsageDTO dto = new VoucherUsageDTO();
        dto.id = usage.getId();
        dto.voucherId = usage.getVoucherId();
        dto.voucherCode = usage.getVoucherCode();
        dto.userId = usage.getUserId();
        dto.orderId = usage.getOrderId();
        dto.orderNumber = usage.getOrderNumber();
        dto.originalAmount = usage.getOriginalAmount();
        dto.discountAmount = usage.getDiscountAmount();
        dto.finalAmount = usage.getFinalAmount();
        dto.usedAt = usage.getUsedAt();
        dto.createdAt = usage.getCreatedAt();
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
