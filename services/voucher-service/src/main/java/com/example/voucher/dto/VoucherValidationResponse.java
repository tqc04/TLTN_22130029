package com.example.voucher.dto;

import com.example.voucher.entity.VoucherType;

import java.math.BigDecimal;

public class VoucherValidationResponse {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private Long voucherId;
    private String voucherCode;
    private VoucherType voucherType;
    private BigDecimal voucherValue;
    private Boolean freeShipping = false;

    public VoucherValidationResponse() {}

    public VoucherValidationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public VoucherValidationResponse(boolean valid, String message, BigDecimal discountAmount,
                                   BigDecimal finalAmount, Long voucherId, String voucherCode,
                                   VoucherType voucherType, BigDecimal voucherValue) {
        this.valid = valid;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.voucherType = voucherType;
        this.voucherValue = voucherValue;
        this.freeShipping = false;
    }
    
    public VoucherValidationResponse(boolean valid, String message, BigDecimal discountAmount,
                                   BigDecimal finalAmount, Long voucherId, String voucherCode,
                                   VoucherType voucherType, BigDecimal voucherValue, Boolean freeShipping) {
        this.valid = valid;
        this.message = message;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.voucherType = voucherType;
        this.voucherValue = voucherValue;
        this.freeShipping = freeShipping != null ? freeShipping : false;
    }

    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public VoucherType getVoucherType() { return voucherType; }
    public void setVoucherType(VoucherType voucherType) { this.voucherType = voucherType; }

    public BigDecimal getVoucherValue() { return voucherValue; }
    public void setVoucherValue(BigDecimal voucherValue) { this.voucherValue = voucherValue; }
    
    public Boolean getFreeShipping() { return freeShipping; }
    public void setFreeShipping(Boolean freeShipping) { this.freeShipping = freeShipping; }
}
