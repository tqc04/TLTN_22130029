package com.example.inventory.controller;

import com.example.inventory.entity.InventoryItem;
import com.example.inventory.dto.InventoryItemDTO;
import com.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping("/reserve")
    public ResponseEntity<Map<String, Object>> reserveInventory(@RequestBody Map<String, Object> request) {
        try {
            String productId = request.get("productId") != null ? request.get("productId").toString() : null;
            Integer quantity = request.get("quantity") != null ? Integer.valueOf(request.get("quantity").toString()) : null;
            String orderId = request.get("orderId") != null ? request.get("orderId").toString() : null;

            if (productId == null || quantity == null || quantity <= 0) {
                logger.error("Invalid reservation request: productId={}, quantity={}, orderId={}", productId, quantity, orderId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "error", "Invalid request: productId and quantity are required, quantity must be > 0"
                ));
            }

            logger.info("Reservation request: productId={}, quantity={}, orderId={}", productId, quantity, orderId);
            boolean success = inventoryService.reserveInventory(productId, quantity, orderId);

            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Inventory reserved successfully"));
            } else {
                String errorMsg = String.format("Insufficient stock or reservation failed for productId=%s, quantity=%d. " +
                    "Please check if product exists in inventory_items table or has sufficient stock in product service.", 
                    productId, quantity);
                logger.warn(errorMsg);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "error", errorMsg,
                    "productId", productId,
                    "requestedQuantity", quantity
                ));
            }
        } catch (Exception e) {
            logger.error("Error reserving inventory: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false, 
                "error", "Error reserving inventory: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> releaseInventory(@RequestBody Map<String, Object> request) {
        try {
            String orderId = (String) request.get("orderId");
            logger.info("Inventory release requested for order: {}", orderId);

            // For single item release, need productId and quantity
            if (request.containsKey("productId") && request.containsKey("quantity")) {
                String productId = request.get("productId").toString();
                Integer quantity = Integer.valueOf(request.get("quantity").toString());
                inventoryService.releaseInventory(productId, quantity, orderId);
            } else {
                // For batch release, need items array
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
                if (items != null) {
                    inventoryService.releaseInventoryBatch(items, orderId);
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Inventory released successfully"));
        } catch (Exception e) {
            logger.error("Error releasing inventory: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/rollback")
    public ResponseEntity<Map<String, Object>> rollbackInventory(@RequestBody Map<String, Object> request) {
        try {
            String orderId = request.get("orderId") != null ? request.get("orderId").toString() : "unknown";
            logger.info("Inventory rollback requested for order: {}", orderId);

            // For batch rollback, need items array
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            if (items != null) {
                inventoryService.releaseInventoryBatch(items, orderId);
            } else {
                // For single item rollback
                if (request.containsKey("productId") && request.containsKey("quantity")) {
                    String productId = request.get("productId").toString();
                    Integer quantity = Integer.valueOf(request.get("quantity").toString());
                    inventoryService.releaseInventory(productId, quantity, orderId);
                } else {
                    // Rollback entire order
                    // Note: Current implementation logs the rollback request.
                    // For full implementation, you would need to:
                    // 1. Track reservations by orderId when reserving inventory
                    // 2. Store order-item mappings in a separate table (e.g., order_reservations)
                    // 3. Query and release all reservations for the given orderId
                    // The rollbackInventoryForOrder method in InventoryService handles this
                    boolean success = inventoryService.rollbackInventoryForOrder(orderId);
                    if (!success) {
                        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to rollback inventory"));
                    }
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Inventory rollback completed successfully"));
        } catch (Exception e) {
            logger.error("Error rolling back inventory: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmInventoryReservation(@RequestBody Map<String, Object> request) {
        try {
            String orderId = (String) request.get("orderId");
            logger.info("Inventory confirmation requested for order: {}", orderId);

            // For single item confirmation, need productId and quantity
            if (request.containsKey("productId") && request.containsKey("quantity")) {
                String productId = request.get("productId").toString();
                Integer quantity = Integer.valueOf(request.get("quantity").toString());
                inventoryService.confirmInventoryReservation(productId, quantity, orderId);
            } else {
                // For batch confirmation, need items array
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
                if (items != null) {
                    inventoryService.confirmInventoryBatch(items, orderId);
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Inventory reservation confirmed"));
        } catch (Exception e) {
            logger.error("Error confirming inventory reservation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/status/{productId}")
    public ResponseEntity<InventoryService.InventoryStatus> getInventoryStatus(@PathVariable String productId) {
        try {
            InventoryService.InventoryStatus status = inventoryService.getInventoryStatus(productId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/check-stock")
    public ResponseEntity<Map<String, Boolean>> checkStock(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        try {
            boolean inStock = inventoryService.isInStock(productId, quantity);
            return ResponseEntity.ok(Map.of("inStock", inStock));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItem>> getLowStockProducts() {
        try {
            List<InventoryItem> items = inventoryService.getLowStockProducts(10);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<InventoryItem>> getOutOfStockProducts() {
        try {
            List<InventoryItem> items = inventoryService.getOutOfStockProducts();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/stock-quantity/{productId}")
    public ResponseEntity<Map<String, Integer>> getStockQuantity(@PathVariable String productId) {
        try {
            Integer quantity = inventoryService.getStockQuantity(productId);
            return ResponseEntity.ok(Map.of("quantity", quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/restock")
    public ResponseEntity<Map<String, Object>> restockProduct(@RequestBody Map<String, Object> request) {
        try {
            String productId = request.get("productId").toString();
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            Long warehouseId = request.get("warehouseId") != null ? Long.valueOf(request.get("warehouseId").toString()) : null;
            
            boolean success = inventoryService.restockProduct(productId, quantity, warehouseId);
            
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Product restocked successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to restock product"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @PostMapping("/create-item")
    public ResponseEntity<Map<String, Object>> createInventoryItem(@RequestBody Map<String, Object> request) {
        try {
            String productId = request.get("productId").toString();
            String warehouseLocation = (String) request.get("warehouseLocation");
            Integer initialQuantity = Integer.valueOf(request.get("initialQuantity").toString());
            
            inventoryService.createInventoryItemIfNotExists(productId, warehouseLocation, initialQuantity);
            return ResponseEntity.ok(Map.of("success", true, "message", "Inventory item created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory API is working!");
    }
    
    /**
     * Get all inventory items with pagination (for admin)
     */
    @GetMapping("")
    public ResponseEntity<org.springframework.data.domain.Page<InventoryItemDTO>> getAllInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<InventoryItem> items = inventoryService.getAllInventory(pageable);
            org.springframework.data.domain.Page<InventoryItemDTO> dtoPage = items.map(InventoryItemDTO::fromEntity);
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            logger.error("Error getting inventory: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Create inventory for new product (called by Product Service)
     * ĐỒNG BỘ DỮ LIỆU: Khi admin tạo sản phẩm mới
     */
    /**
     * Batch reserve inventory for all order items
     */
    @PostMapping("/reserve-batch")
    public ResponseEntity<Map<String, Object>> reserveInventoryBatch(@RequestBody Map<String, Object> request) {
        try {
            String orderId = (String) request.get("orderId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No items provided"));
            }

            boolean success = inventoryService.reserveInventoryBatch(items, orderId);

            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "All inventory items reserved successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Failed to reserve some inventory items"));
            }
        } catch (Exception e) {
            logger.error("Error reserving inventory batch: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Batch confirm inventory for all order items
     */
    @PostMapping("/confirm-batch")
    public ResponseEntity<Map<String, Object>> confirmInventoryBatch(@RequestBody Map<String, Object> request) {
        try {
            String orderId = (String) request.get("orderId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No items provided"));
            }

            inventoryService.confirmInventoryBatch(items, orderId);

            return ResponseEntity.ok(Map.of("success", true, "message", "All inventory items confirmed successfully"));
        } catch (Exception e) {
            logger.error("Error confirming inventory batch: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createInventory(@RequestBody Map<String, Object> request) {
        try {
            String productId = request.get("productId").toString();
            String productName = (String) request.get("productName");
            Integer initialStock = Integer.valueOf(request.get("initialStock").toString());
            String warehouseLocation = (String) request.getOrDefault("warehouseLocation", "Main Warehouse");
            
            boolean success = inventoryService.createInventoryForProduct(productId, productName, initialStock, warehouseLocation);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Inventory created and synced",
                    "productId", productId,
                    "initialStock", initialStock
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to create inventory"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Sync stock from Product Service
     * ĐỒNG BỘ DỮ LIỆU: Khi admin update stock của sản phẩm
     */
    @PostMapping("/sync-stock")
    public ResponseEntity<Map<String, Object>> syncStock(@RequestBody Map<String, Object> request) {
        try {
            String productId = request.get("productId").toString();
            Integer stockQuantity = Integer.valueOf(request.get("stockQuantity").toString());
            
            boolean success = inventoryService.syncStockFromProduct(productId, stockQuantity);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Stock synced successfully",
                    "productId", productId,
                    "stockQuantity", stockQuantity
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Failed to sync stock"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Sync all products from Product Service to inventory_items
     * ĐỒNG BỘ TẤT CẢ SẢN PHẨM: Tạo inventory items cho tất cả sản phẩm chưa có trong inventory
     */
    @PostMapping("/sync-all-products")
    public ResponseEntity<Map<String, Object>> syncAllProducts() {
        try {
            logger.info("Starting sync all products from product service to inventory_items...");
            Map<String, Object> result = inventoryService.syncAllProductsFromProductService();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error syncing all products: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to sync all products: " + e.getMessage()
            ));
        }
    }
}
