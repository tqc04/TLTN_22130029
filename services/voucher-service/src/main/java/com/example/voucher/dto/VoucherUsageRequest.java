package com.example.voucher.dto;

import java.math.BigDecimal;

public class VoucherUsageRequest {
    private Long voucherId;
    private String voucherCode;
    private String userId;
    private Long orderId;
    private String orderNumber;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public VoucherUsageRequest() {}

    public VoucherUsageRequest(Long voucherId, String voucherCode, String userId, Long orderId,
                             String orderNumber, BigDecimal originalAmount, BigDecimal discountAmount,
                             BigDecimal finalAmount) {
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.userId = userId;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    // Getters and Setters
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
}
