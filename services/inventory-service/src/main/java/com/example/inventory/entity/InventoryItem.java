package com.example.inventory.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@EntityListeners(AuditingEntityListener.class)
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    
    @Column(name = "warehouse_location")
    private String warehouseLocation;
    
    @Column(name = "quantity_on_hand")
    private Integer quantityOnHand = 0;
    
    @Column(name = "quantity_reserved")
    private Integer quantityReserved = 0;
    
    @Column(name = "quantity_available")
    private Integer quantityAvailable = 0;
    
    @Column(name = "min_stock_level")
    private Integer minStockLevel = 10;
    
    @Column(name = "max_stock_level")
    private Integer maxStockLevel = 1000;
    
    @Column(name = "reorder_point")
    private Integer reorderPoint = 20;
    
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity = 50;
    
    @Column(name = "last_restock_date")
    private LocalDateTime lastRestockDate;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public InventoryItem() {}
    
    public InventoryItem(String productId, String warehouseLocation, Integer initialQuantity) {
        this.productId = productId;
        this.warehouseLocation = warehouseLocation;
        this.quantityOnHand = initialQuantity;
        this.quantityAvailable = initialQuantity;
        this.quantityReserved = 0;
    }
    
    // Business methods
    public void reserveStock(Integer quantity) {
        if (quantity > 0 && quantity <= this.quantityAvailable) {
            this.quantityReserved += quantity;
            this.quantityAvailable -= quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void releaseReservedStock(Integer quantity) {
        if (quantity > 0 && quantity <= this.quantityReserved) {
            this.quantityReserved -= quantity;
            this.quantityAvailable += quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void confirmReservedStock(Integer quantity) {
        if (quantity > 0 && quantity <= this.quantityReserved) {
            this.quantityReserved -= quantity;
            this.quantityOnHand -= quantity;  // Actually remove from inventory
            // quantityAvailable stays the same (already reduced when reserved)
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public boolean isLowStock() {
        return this.quantityAvailable <= this.minStockLevel;
    }
    
    public boolean isOutOfStock() {
        return this.quantityAvailable <= 0;
    }
    
    public boolean needsReorder() {
        return this.quantityAvailable <= this.reorderPoint;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getWarehouseLocation() {
        return warehouseLocation;
    }
    
    public void setWarehouseLocation(String warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }
    
    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }
    
    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }
    
    public Integer getQuantityReserved() {
        return quantityReserved;
    }
    
    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved;
    }
    
    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }
    
    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }
    
    public Integer getMinStockLevel() {
        return minStockLevel;
    }
    
    public void setMinStockLevel(Integer minStockLevel) {
        this.minStockLevel = minStockLevel;
    }
    
    public Integer getMaxStockLevel() {
        return maxStockLevel;
    }
    
    public void setMaxStockLevel(Integer maxStockLevel) {
        this.maxStockLevel = maxStockLevel;
    }
    
    public Integer getReorderPoint() {
        return reorderPoint;
    }
    
    public void setReorderPoint(Integer reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
    
    public Integer getReorderQuantity() {
        return reorderQuantity;
    }
    
    public void setReorderQuantity(Integer reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }
    
    public LocalDateTime getLastRestockDate() {
        return lastRestockDate;
    }
    
    public void setLastRestockDate(LocalDateTime lastRestockDate) {
        this.lastRestockDate = lastRestockDate;
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
