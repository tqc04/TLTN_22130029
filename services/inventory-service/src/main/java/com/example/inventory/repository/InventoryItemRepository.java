package com.example.inventory.repository;

import com.example.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    
    Optional<InventoryItem> findByProductId(String productId);
    
    @Query("SELECT i FROM InventoryItem i WHERE i.quantityAvailable <= i.minStockLevel")
    List<InventoryItem> findLowStockItems();
    
    @Query("SELECT i FROM InventoryItem i WHERE i.quantityAvailable <= 0")
    List<InventoryItem> findOutOfStockItems();
    
    @Query("SELECT i FROM InventoryItem i WHERE i.quantityAvailable <= i.reorderPoint")
    List<InventoryItem> findItemsNeedingReorder();
    
    @Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId AND i.quantityAvailable >= :quantity")
    Optional<InventoryItem> findAvailableItem(@Param("productId") String productId, @Param("quantity") Integer quantity);
}
