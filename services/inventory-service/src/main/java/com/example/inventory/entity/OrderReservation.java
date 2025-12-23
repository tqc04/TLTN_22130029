package com.example.inventory.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity to track inventory reservations by order
 * This allows proper rollback when orders are cancelled
 */
@Entity
@Table(name = "order_reservations", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_product_id", columnList = "product_id")
})
@EntityListeners(AuditingEntityListener.class)
public class OrderReservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;
    
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status = ReservationStatus.RESERVED;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "released_at")
    private LocalDateTime releasedAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    // Constructors
    public OrderReservation() {}
    
    public OrderReservation(String orderId, String productId, Integer quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationStatus.RESERVED;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public ReservationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }
    
    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }
    
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
    
    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
    
    /**
     * Mark reservation as released (rollback)
     */
    public void markAsReleased() {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
    }
    
    /**
     * Mark reservation as confirmed (order confirmed)
     */
    public void markAsConfirmed() {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
    
    public enum ReservationStatus {
        RESERVED,   // Inventory reserved but order not confirmed
        CONFIRMED,  // Order confirmed, inventory actually deducted
        RELEASED    // Reservation released (rollback)
    }
}
