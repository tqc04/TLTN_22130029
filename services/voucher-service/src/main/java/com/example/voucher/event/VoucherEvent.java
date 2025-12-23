package com.example.voucher.event;

import com.example.voucher.entity.VoucherUsage;

import java.time.LocalDateTime;

public class VoucherEvent {
    private String eventType; // VOUCHER_APPLIED, VOUCHER_USED, VOUCHER_EXPIRED
    private String voucherId;
    private String voucherCode;
    private String userId;
    private Long orderId;
    private String orderNumber;
    private Double discountAmount;
    private Double originalAmount;
    private Double finalAmount;
    private LocalDateTime timestamp;
    private String source; // CART, ORDER, ADMIN

    // Constructors
    public VoucherEvent() {}

    public VoucherEvent(String eventType, VoucherUsage usage, String source) {
        this.eventType = eventType;
        this.voucherId = usage.getVoucherId() != null ? usage.getVoucherId().toString() : null;
        this.voucherCode = usage.getVoucherCode();
        this.userId = usage.getUserId();
        this.orderId = usage.getOrderId();
        this.orderNumber = usage.getOrderNumber();
        this.discountAmount = usage.getDiscountAmount().doubleValue();
        this.originalAmount = usage.getOriginalAmount().doubleValue();
        this.finalAmount = usage.getFinalAmount().doubleValue();
        this.timestamp = LocalDateTime.now();
        this.source = source;
    }

    public VoucherEvent(String eventType, String voucherId, String voucherCode, String userId, String source) {
        this.eventType = eventType;
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
        this.source = source;
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
