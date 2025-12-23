package com.example.inventory.dto;

import com.example.inventory.entity.InventoryItem;
import java.time.LocalDateTime;

public class InventoryItemDTO {
    private Long id;
    private String productId;
    private String productName;
    private String productSku;
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer reorderLevel;
    private Integer reorderQuantity;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public InventoryItemDTO() {}
    
    public static InventoryItemDTO fromEntity(InventoryItem item) {
        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setProductName("Product #" + item.getProductId()); // Placeholder
        dto.setProductSku("SKU-" + item.getProductId()); // Placeholder
        dto.setWarehouseId(1L); // Default warehouse
        dto.setWarehouseName(item.getWarehouseLocation() != null ? item.getWarehouseLocation() : "Main Warehouse");
        dto.setQuantity(item.getQuantityOnHand());
        dto.setReservedQuantity(item.getQuantityReserved());
        dto.setAvailableQuantity(item.getQuantityAvailable());
        dto.setReorderLevel(item.getReorderPoint());
        dto.setReorderQuantity(item.getReorderQuantity());
        dto.setLastRestockedAt(item.getLastRestockDate());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
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
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductSku() {
        return productSku;
    }
    
    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }
    
    public Long getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public String getWarehouseName() {
        return warehouseName;
    }
    
    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Integer getReservedQuantity() {
        return reservedQuantity;
    }
    
    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }
    
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    
    public Integer getReorderLevel() {
        return reorderLevel;
    }
    
    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
    
    public Integer getReorderQuantity() {
        return reorderQuantity;
    }
    
    public void setReorderQuantity(Integer reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }
    
    public LocalDateTime getLastRestockedAt() {
        return lastRestockedAt;
    }
    
    public void setLastRestockedAt(LocalDateTime lastRestockedAt) {
        this.lastRestockedAt = lastRestockedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

