package com.example.voucher.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers", indexes = {
    @Index(name = "idx_user_voucher_user", columnList = "user_id"),
    @Index(name = "idx_user_voucher_voucher", columnList = "voucher_id"),
    @Index(name = "idx_user_voucher_user_voucher", columnList = "user_id,voucher_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class UserVoucher {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;
    
    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;
    
    @Column(name = "voucher_code", nullable = false)
    private String voucherCode;
    
    @Column(name = "obtained_at", nullable = false)
    private LocalDateTime obtainedAt;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "order_number")
    private String orderNumber;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public UserVoucher() {}
    
    public UserVoucher(String userId, Long voucherId, String voucherCode) {
        this.userId = userId;
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.obtainedAt = LocalDateTime.now();
        this.isUsed = false;
    }
    
    // Business methods
    public void markAsUsed(Long orderId, String orderNumber) {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }
    
    public boolean isExpired(LocalDateTime voucherEndDate) {
        if (voucherEndDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(voucherEndDate);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Long getVoucherId() {
        return voucherId;
    }
    
    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }
    
    public String getVoucherCode() {
        return voucherCode;
    }
    
    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
    
    public LocalDateTime getObtainedAt() {
        return obtainedAt;
    }
    
    public void setObtainedAt(LocalDateTime obtainedAt) {
        this.obtainedAt = obtainedAt;
    }
    
    public LocalDateTime getUsedAt() {
        return usedAt;
    }
    
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
    
    public Boolean getIsUsed() {
        return isUsed;
    }
    
    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

