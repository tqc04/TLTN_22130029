package com.example.inventory.service;

import com.example.inventory.entity.InventoryItem;
import com.example.inventory.entity.OrderReservation;
import com.example.inventory.repository.InventoryItemRepository;
import com.example.inventory.repository.OrderReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    
    @Autowired
    private OrderReservationRepository orderReservationRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.product.base-url:http://localhost:8083}")
    private String productServiceUrl;
    
    // In-memory locks for inventory items
    private final ConcurrentHashMap<String, ReentrantLock> inventoryLocks = new ConcurrentHashMap<>();
    
    /**
     * Reserve inventory for order (with lock)
     */
    @Transactional
    public boolean reserveInventory(String productId, Integer quantity, String orderId) {
        if (productId == null || quantity == null || quantity <= 0) {
            logger.error("Invalid parameters for inventory reservation: productId={}, quantity={}, orderId={}", 
                productId, quantity, orderId);
            return false;
        }
        
        ReentrantLock lock = inventoryLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        
        try {
            lock.lock();
            
            // Prefer inventory_items
            Optional<InventoryItem> invOpt = inventoryItemRepository.findByProductId(productId);
            if (invOpt.isPresent()) {
                InventoryItem item = invOpt.get();
                Integer available = item.getQuantityAvailable() != null ? item.getQuantityAvailable() : 0;
                if (available < quantity) {
                    logger.warn("Insufficient stock (inventory_items) for reservation: productId={}, requested={}, available={}, orderId={}", 
                        productId, quantity, available, orderId);
                    return false;
                }
                item.reserveStock(quantity);
                inventoryItemRepository.save(item);
                
                // Track reservation for rollback capability
                if (orderId != null && !orderId.trim().isEmpty()) {
                    OrderReservation reservation = new OrderReservation(orderId, productId, quantity);
                    orderReservationRepository.save(reservation);
                    logger.debug("Created order reservation: orderId={}, productId={}, quantity={}", 
                            orderId, productId, quantity);
                }
                
                logger.info("Reserved in inventory_items: productId={}, qty={}, available->{}", productId, quantity, item.getQuantityAvailable());
                return true;
            }

            // Fallback to product stock via Product Service
            logger.info("Product {} not found in inventory_items, checking product service...", productId);
            try {
                var productResponse = restTemplate.getForObject(productServiceUrl + "/api/products/" + productId, java.util.Map.class);
                if (productResponse != null) {
                    Object stockObj = productResponse.get("stockQuantity");
                    Integer currentStock = null;
                    
                    // Handle different number types
                    if (stockObj instanceof Integer) {
                        currentStock = (Integer) stockObj;
                    } else if (stockObj instanceof Number) {
                        currentStock = ((Number) stockObj).intValue();
                    }
                    
                    logger.info("Product service response for {}: stockQuantity={}, requested={}", 
                        productId, currentStock, quantity);
                    
                    if (currentStock != null && currentStock >= quantity) {
                        // Auto-create inventory item for future reservations
                        try {
                            String productName = (String) productResponse.getOrDefault("name", "Product-" + productId);
                            createInventoryForProduct(productId, productName, currentStock, "Main Warehouse");
                            logger.info("Auto-created inventory item for productId={} with stock={}", productId, currentStock);
                        } catch (Exception e) {
                            logger.warn("Failed to auto-create inventory item for productId={}: {}", productId, e.getMessage());
                        }
                        
                        // Reserve from the newly created inventory item
                        Optional<InventoryItem> newInvOpt = inventoryItemRepository.findByProductId(productId);
                        if (newInvOpt.isPresent()) {
                            InventoryItem newItem = newInvOpt.get();
                            Integer available = newItem.getQuantityAvailable() != null ? newItem.getQuantityAvailable() : 0;
                            if (available >= quantity) {
                                newItem.reserveStock(quantity);
                                inventoryItemRepository.save(newItem);
                                
                                // Track reservation
                                if (orderId != null && !orderId.trim().isEmpty()) {
                                    OrderReservation reservation = new OrderReservation(orderId, productId, quantity);
                                    orderReservationRepository.save(reservation);
                                }
                                
                                logger.info("Reserved in auto-created inventory_items: productId={}, qty={}, available->{}", 
                                    productId, quantity, newItem.getQuantityAvailable());
                                return true;
                            }
                        }
                        
                        // Fallback: Update product stock via Product Service (legacy method)
                        java.util.Map<String, Object> updateRequest = new java.util.HashMap<>();
                        updateRequest.put("stockQuantity", currentStock - quantity);
                        
                        restTemplate.put(productServiceUrl + "/api/products/" + productId + "/stock", updateRequest);
                        
                        logger.info("Inventory reserved (product): productId={}, quantity={}, oldStock={}, newStock={}, orderId={}", 
                            productId, quantity, currentStock, currentStock - quantity, orderId);
                        return true;
                    } else {
                        logger.warn("Insufficient stock from product service: productId={}, requested={}, available={}, orderId={}", 
                            productId, quantity, currentStock, orderId);
                    }
                } else {
                    logger.warn("Product service returned null response for productId={}", productId);
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                logger.error("HTTP error calling Product Service for productId={}: status={}, body={}", 
                    productId, e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                logger.error("Error calling Product Service for productId={}: {}", productId, e.getMessage(), e);
            }
            
            logger.warn("Insufficient stock for reservation: productId={}, requested={}, orderId={}. " +
                "Product not found in inventory_items and product service check failed or insufficient stock.", 
                productId, quantity, orderId);
            return false;

        } catch (Exception e) {
            logger.error("Error during inventory reservation: productId={}, quantity={}, orderId={}, error={}", 
                productId, quantity, orderId, e.getMessage(), e);
            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Release reserved inventory (when order is cancelled)
     */
    @Transactional
    public void releaseInventory(String productId, Integer quantity, String orderId) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        
        ReentrantLock lock = inventoryLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        
        try {
            lock.lock();
            
            Optional<InventoryItem> invOpt = inventoryItemRepository.findByProductId(productId);
            if (invOpt.isPresent()) {
                InventoryItem item = invOpt.get();
                item.releaseReservedStock(quantity);
                inventoryItemRepository.save(item);
                logger.info("Released in inventory_items: productId={}, qty={}, available->{}", productId, quantity, item.getQuantityAvailable());
                return;
            }

            // Fallback to product stock via Product Service
            try {
                var productResponse = restTemplate.getForObject(productServiceUrl + "/api/products/" + productId, java.util.Map.class);
                if (productResponse != null) {
                    Integer currentStock = (Integer) productResponse.get("stockQuantity");
                    if (currentStock != null) {
                        java.util.Map<String, Object> updateRequest = new java.util.HashMap<>();
                        updateRequest.put("stockQuantity", currentStock + quantity);
                        
                        restTemplate.put(productServiceUrl + "/api/products/" + productId + "/stock", updateRequest);
                        
                        logger.info("Inventory released (product): productId={}, qty={}, newStock={}", productId, quantity, currentStock + quantity);
                    }
                }
            } catch (Exception e) {
                logger.error("Error calling Product Service: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error during inventory release: productId={}, quantity={}, orderId={}, error={}", 
                productId, quantity, orderId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Confirm inventory reservation (when order is confirmed)
     * This actually reduces the on-hand quantity and removes from reserved
     */
    @Transactional
    public void confirmInventoryReservation(String productId, Integer quantity, String orderId) {
        // Mark reservation as confirmed
        if (orderId != null && !orderId.trim().isEmpty()) {
            List<OrderReservation> reservations = orderReservationRepository.findByOrderIdAndStatus(
                    orderId, OrderReservation.ReservationStatus.RESERVED);
            for (OrderReservation reservation : reservations) {
                if (reservation.getProductId().equals(productId) && 
                    reservation.getQuantity().equals(quantity)) {
                    reservation.markAsConfirmed();
                    orderReservationRepository.save(reservation);
                    break;
                }
            }
        }
        if (productId == null || quantity == null || quantity <= 0) {
            logger.warn("Invalid parameters for inventory confirmation: productId={}, quantity={}, orderId={}", 
                productId, quantity, orderId);
            return;
        }
        
        ReentrantLock lock = inventoryLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        
        try {
            lock.lock();
            
            Optional<InventoryItem> invOpt = inventoryItemRepository.findByProductId(productId);
            if (invOpt.isPresent()) {
                InventoryItem item = invOpt.get();
                item.confirmReservedStock(quantity);
                inventoryItemRepository.save(item);
                logger.info("Confirmed inventory reservation (inventory_items): productId={}, qty={}, remaining reserved={}, on-hand={}", 
                    productId, quantity, item.getQuantityReserved(), item.getQuantityOnHand());
            } else {
                // If not in inventory_items, the stock was already reduced from product during reservation
                logger.info("Confirmed inventory reservation (product stock already reduced): productId={}, qty={}", 
                    productId, quantity);
            }
        } catch (Exception e) {
            logger.error("Error during inventory confirmation: productId={}, quantity={}, orderId={}, error={}", 
                productId, quantity, orderId, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Rollback all inventory reservations for an order
     * This is called when order creation fails or order is cancelled
     */
    @Transactional
    public boolean rollbackInventoryForOrder(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            logger.warn("Invalid orderId for rollback: {}", orderId);
            return false;
        }

        try {
            // Find all reservations for this order
            List<OrderReservation> reservations = orderReservationRepository.findByOrderIdAndStatus(
                    orderId, OrderReservation.ReservationStatus.RESERVED);
            
            if (reservations.isEmpty()) {
                logger.info("No active reservations found for order: {}", orderId);
                return true; // No reservations to rollback
            }
            
            logger.info("Rolling back {} reservations for order: {}", reservations.size(), orderId);
            
            // Release each reservation
            for (OrderReservation reservation : reservations) {
                try {
                    String productId = reservation.getProductId();
                    Integer quantity = reservation.getQuantity();
                    
                    // Release inventory
                    releaseInventory(productId, quantity, orderId);
                    
                    // Mark reservation as released
                    reservation.markAsReleased();
                    orderReservationRepository.save(reservation);
                    
                    logger.debug("Released reservation: orderId={}, productId={}, quantity={}", 
                            orderId, productId, quantity);
                } catch (Exception e) {
                    logger.error("Error releasing reservation {} for order {}: {}", 
                            reservation.getId(), orderId, e.getMessage());
                    // Continue with other reservations
                }
            }
            
            logger.info("Successfully rolled back {} reservations for order: {}", 
                    reservations.size(), orderId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error during inventory rollback for order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reserve inventory for multiple products (batch operation for orders)
     */
    @Transactional
    public boolean reserveInventoryBatch(List<Map<String, Object>> items, String orderId) {
        if (items == null || items.isEmpty()) {
            logger.error("No items provided for inventory reservation: orderId={}", orderId);
            return false;
        }

        try {
            boolean allReserved = true;

            for (Map<String, Object> item : items) {
                String productId = item.get("productId").toString();
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                boolean reserved = reserveInventory(productId, quantity, orderId);
                if (!reserved) {
                    logger.error("Failed to reserve inventory for product {} in order {}", productId, orderId);
                    allReserved = false;
                    break;
                }
            }

            if (!allReserved) {
                // Rollback already reserved items
                releaseInventoryBatch(items, orderId);
                return false;
            }

            logger.info("Successfully reserved inventory for all items in order: {}", orderId);
            return true;

        } catch (Exception e) {
            logger.error("Error during batch inventory reservation for order {}: {}", orderId, e.getMessage(), e);
            // Rollback any partial reservations
            releaseInventoryBatch(items, orderId);
            return false;
        }
    }

    /**
     * Release inventory for multiple products (rollback batch operation)
     */
    @Transactional
    public void releaseInventoryBatch(List<Map<String, Object>> items, String orderId) {
        if (items == null || items.isEmpty()) {
            logger.warn("No items provided for inventory release: orderId={}", orderId);
            return;
        }

        try {
            for (Map<String, Object> item : items) {
                String productId = item.get("productId").toString();
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                releaseInventory(productId, quantity, orderId);
            }

            logger.info("Successfully released inventory for all items in order: {}", orderId);

        } catch (Exception e) {
            logger.error("Error during batch inventory release for order {}: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * Confirm inventory reservation for multiple products (when order is confirmed)
     */
    @Transactional
    public void confirmInventoryBatch(List<Map<String, Object>> items, String orderId) {
        if (items == null || items.isEmpty()) {
            logger.warn("No items provided for inventory confirmation: orderId={}", orderId);
            return;
        }

        try {
            for (Map<String, Object> item : items) {
                String productId = item.get("productId").toString();
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                confirmInventoryReservation(productId, quantity, orderId);
            }

            logger.info("Successfully confirmed inventory for all items in order: {}", orderId);

        } catch (Exception e) {
            logger.error("Error during batch inventory confirmation for order {}: {}", orderId, e.getMessage(), e);
        }
    }
    
    /**
     * Get current stock status
     */
    public InventoryStatus getInventoryStatus(String productId) {
        // Prefer inventory_items if present
        Optional<InventoryItem> invOpt = inventoryItemRepository.findByProductId(productId);
        if (invOpt.isPresent()) {
            InventoryItem item = invOpt.get();
            Integer onHand = item.getQuantityOnHand() != null ? item.getQuantityOnHand() : 0;
            Integer reserved = item.getQuantityReserved() != null ? item.getQuantityReserved() : 0;
            Integer available = item.getQuantityAvailable() != null ? item.getQuantityAvailable() : Math.max(0, onHand - reserved);
            int total = available + reserved;
            return new InventoryStatus(total, available, reserved, 0);
        }
        
        // Fallback to product stock via Product Service
        try {
            var productResponse = restTemplate.getForObject(productServiceUrl + "/api/products/" + productId, java.util.Map.class);
            if (productResponse != null) {
                Integer stock = (Integer) productResponse.get("stockQuantity");
                if (stock != null) {
                    return new InventoryStatus(stock, stock, 0, 0);
                }
            }
        } catch (Exception e) {
            logger.error("Error calling Product Service: {}", e.getMessage());
        }
        
        return new InventoryStatus(0, 0, 0, 0);
    }
    
    /**
     * Check if product is in stock
     */
    public boolean isInStock(String productId, Integer quantity) {
        InventoryStatus status = getInventoryStatus(productId);
        return status.availableQuantity >= quantity;
    }
    
    /**
     * Get low stock products
     */
    public List<InventoryItem> getLowStockProducts(Integer threshold) {
        return inventoryItemRepository.findLowStockItems();
    }
    
    /**
     * Get out of stock products
     */
    public List<InventoryItem> getOutOfStockProducts() {
        return inventoryItemRepository.findOutOfStockItems();
    }
    
    /**
     * Get all inventory items with pagination (for admin)
     */
    public org.springframework.data.domain.Page<InventoryItem> getAllInventory(org.springframework.data.domain.Pageable pageable) {
        return inventoryItemRepository.findAll(pageable);
    }
    
    /**
     * Get total available quantity for a product
     */
    public Integer getTotalAvailableQuantity(String productId) {
        try {
            InventoryStatus status = getInventoryStatus(productId);
            return status.availableQuantity;
        } catch (Exception e) {
            logger.error("Error getting inventory for product {}: {}", productId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get stock quantity for a product (alias for getTotalAvailableQuantity)
     */
    public Integer getStockQuantity(String productId) {
        return getTotalAvailableQuantity(productId);
    }
    
    /**
     * Create inventory item for a product if it doesn't exist
     */
    @Transactional
    public void createInventoryItemIfNotExists(String productId, String warehouseLocation, Integer initialQuantity) {
        Optional<InventoryItem> existingItem = inventoryItemRepository.findByProductId(productId);
        if (existingItem.isPresent()) {
            // Update existing inventory
            InventoryItem inventoryItem = existingItem.get();
            inventoryItem.setQuantityOnHand(initialQuantity);
            inventoryItem.setQuantityAvailable(initialQuantity);
            inventoryItem.setQuantityReserved(0); // Reset reserved quantity
            inventoryItem.setWarehouseLocation(warehouseLocation);
            inventoryItem.setLastRestockDate(LocalDateTime.now());
            inventoryItem.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(inventoryItem);
            logger.info("Updated inventory item for product: productId={}", productId);
        } else {
            // Create new inventory item
            InventoryItem inventoryItem = new InventoryItem(productId, warehouseLocation, initialQuantity);
            inventoryItemRepository.save(inventoryItem);
            logger.info("Created inventory item for product: productId={}", productId);
        }
    }
    
    /**
     * Restock product - add quantity to inventory
     */
    @Transactional
    public boolean restockProduct(String productId, Integer quantity, Long warehouseId) {
        try {
            ReentrantLock lock = inventoryLocks.computeIfAbsent(productId, k -> new ReentrantLock());
            
            try {
                lock.lock();
                
                // Find existing inventory item or create new one
                Optional<InventoryItem> itemOpt = inventoryItemRepository.findByProductId(productId);
                
                if (itemOpt.isPresent()) {
                    // Update existing inventory item
                    InventoryItem item = itemOpt.get();
                    
                    Integer currentOnHand = item.getQuantityOnHand() != null ? item.getQuantityOnHand() : 0;
                    Integer currentReserved = item.getQuantityReserved() != null ? item.getQuantityReserved() : 0;
                    
                    item.setQuantityOnHand(currentOnHand + quantity);
                    item.setQuantityAvailable((currentOnHand + quantity) - currentReserved);
                    item.setLastRestockDate(LocalDateTime.now());
                    item.setUpdatedAt(LocalDateTime.now());
                    inventoryItemRepository.save(item);
                    
                    logger.info("Updated inventory item: productId={}, addedQty={}, newOnHand={}, newAvailable={}", 
                        productId, quantity, item.getQuantityOnHand(), item.getQuantityAvailable());
                } else {
                    // Create new inventory item if none exists
                    InventoryItem item = new InventoryItem();
                    item.setProductId(productId); // Fixed: was hardcoded "productId"
                    item.setWarehouseLocation("Main Warehouse");
                    item.setQuantityOnHand(quantity);
                    item.setQuantityAvailable(quantity);
                    item.setQuantityReserved(0);
                    item.setMinStockLevel(10); // default
                    item.setMaxStockLevel(1000); // default
                    item.setReorderPoint(20); // default
                    item.setReorderQuantity(50); // default
                    item.setLastRestockDate(LocalDateTime.now());
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUpdatedAt(LocalDateTime.now());
                    
                    inventoryItemRepository.save(item);
                    logger.info("Created new inventory item: productId={}, initialQty={}", productId, quantity);
                }
                
                // Also update product stock via Product Service
                try {
                    var productResponse = restTemplate.getForObject(productServiceUrl + "/api/products/" + productId, java.util.Map.class);
                    if (productResponse != null) {
                        Integer currentStock = (Integer) productResponse.get("stockQuantity");
                        if (currentStock != null) {
                            java.util.Map<String, Object> updateRequest = new java.util.HashMap<>();
                            updateRequest.put("stockQuantity", currentStock + quantity);
                            
                            restTemplate.put(productServiceUrl + "/api/products/" + productId + "/stock", updateRequest);
                            
                            logger.info("Updated product stock: productId={}, oldStock={}, newStock={}", 
                                productId, currentStock, currentStock + quantity);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error calling Product Service: {}", e.getMessage());
                }
                
                return true;
                
            } finally {
                lock.unlock();
            }
            
        } catch (Exception e) {
            logger.error("Failed to restock product {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Failed to restock product " + productId + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Create inventory for new product (called by Product Service)
     * ĐỒNG BỘ: Tạo inventory record khi admin tạo sản phẩm mới
     */
    @Transactional
    public boolean createInventoryForProduct(String productId, String productName, Integer initialStock, String warehouseLocation) {
        try {
            // Check if inventory already exists
            Optional<InventoryItem> existing = inventoryItemRepository.findByProductId(productId);
            if (existing.isPresent()) {
                // Update existing inventory instead of just returning
                InventoryItem item = existing.get();
                item.setQuantityOnHand(initialStock != null ? initialStock : 0);
                item.setQuantityAvailable(initialStock != null ? initialStock : 0);
                item.setQuantityReserved(0); // Reset reserved quantity
                item.setWarehouseLocation(warehouseLocation != null ? warehouseLocation : "Main Warehouse");
                item.setLastRestockDate(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                inventoryItemRepository.save(item);

                logger.info("Updated existing inventory for product: productId={}, newStock={}", productId, initialStock);
                return true;
            }
            
            // Create new inventory item
            InventoryItem item = new InventoryItem();
            item.setProductId(productId); // Fixed: was hardcoded "productId"
            item.setWarehouseLocation(warehouseLocation != null ? warehouseLocation : "Main Warehouse");
            item.setQuantityOnHand(initialStock != null ? initialStock : 0);
            item.setQuantityAvailable(initialStock != null ? initialStock : 0);
            item.setQuantityReserved(0);
            item.setMinStockLevel(10);
            item.setReorderPoint(20);
            item.setLastRestockDate(LocalDateTime.now());
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            
            inventoryItemRepository.save(item);
            
            logger.info("✅ Created inventory for new product: productId={}, initialStock={}, warehouse={}", 
                productId, initialStock, warehouseLocation);
            return true;
        } catch (Exception e) {
            logger.error("❌ Failed to create inventory for product {}: {}", productId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Sync stock from Product Service
     * ĐỒNG BỘ: Update inventory khi admin thay đổi stock trong Product Service
     */
    @Transactional
    public boolean syncStockFromProduct(String productId, Integer newStockQuantity) {
        try {
            ReentrantLock lock = inventoryLocks.computeIfAbsent(productId, k -> new ReentrantLock());
            
            try {
                lock.lock();
                
                Optional<InventoryItem> itemOpt = inventoryItemRepository.findByProductId(productId);
                
                if (itemOpt.isPresent()) {
                    InventoryItem item = itemOpt.get();
                    
                    // Calculate difference
                    Integer oldOnHand = item.getQuantityOnHand() != null ? item.getQuantityOnHand() : 0;
                    Integer reserved = item.getQuantityReserved() != null ? item.getQuantityReserved() : 0;
                    
                    // Update quantities
                    item.setQuantityOnHand(newStockQuantity);
                    item.setQuantityAvailable(newStockQuantity - reserved);
                    item.setUpdatedAt(LocalDateTime.now());
                    
                    inventoryItemRepository.save(item);
                    
                    logger.info("✅ Synced stock for product: productId={}, oldStock={}, newStock={}, reserved={}, available={}", 
                        productId, oldOnHand, newStockQuantity, reserved, item.getQuantityAvailable());
                    return true;
                } else {
                    // Inventory doesn't exist, create it
                    logger.warn("Inventory not found for product {}, creating new inventory", productId);
                    return createInventoryForProduct(productId, "Product-" + productId, newStockQuantity, "Main Warehouse");
                }
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            logger.error("❌ Failed to sync stock for product {}: {}", productId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Sync all products from Product Service to inventory_items
     * Fetches all active products and creates inventory items for those that don't exist
     */
    @Transactional
    public Map<String, Object> syncAllProductsFromProductService() {
        int totalProducts = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int errors = 0;
        
        try {
            logger.info("Fetching all products from product service...");
            
            // Fetch all products from product service (with pagination)
            int page = 0;
            int size = 100;
            boolean hasMore = true;
            
            while (hasMore) {
                try {
                    String url = productServiceUrl + "/api/products?page=" + page + "&size=" + size + "&sort=createdAt,desc";
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                    
                    if (response == null || !response.containsKey("content")) {
                        logger.warn("No content in product service response at page {}", page);
                        break;
                    }
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> products = (List<Map<String, Object>>) response.get("content");
                    
                    if (products == null || products.isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    
                    logger.info("Processing page {} with {} products...", page, products.size());
                    
                    for (Map<String, Object> product : products) {
                        totalProducts++;
                        try {
                            String productId = (String) product.get("id");
                            if (productId == null || productId.trim().isEmpty()) {
                                skipped++;
                                continue;
                            }
                            
                            // Check if inventory item already exists
                            Optional<InventoryItem> existingOpt = inventoryItemRepository.findByProductId(productId);
                            
                            // Get stock quantity
                            Object stockObj = product.get("stockQuantity");
                            Integer stockQuantity = null;
                            if (stockObj instanceof Integer) {
                                stockQuantity = (Integer) stockObj;
                            } else if (stockObj instanceof Number) {
                                stockQuantity = ((Number) stockObj).intValue();
                            }
                            
                            // Default to 0 if stockQuantity is null
                            if (stockQuantity == null) {
                                stockQuantity = 0;
                            }
                            
                            if (existingOpt.isPresent()) {
                                // Update existing inventory
                                InventoryItem item = existingOpt.get();
                                Integer oldStock = item.getQuantityOnHand() != null ? item.getQuantityOnHand() : 0;
                                
                                // Only update if stock changed
                                if (!oldStock.equals(stockQuantity)) {
                                    Integer reserved = item.getQuantityReserved() != null ? item.getQuantityReserved() : 0;
                                    item.setQuantityOnHand(stockQuantity);
                                    item.setQuantityAvailable(Math.max(0, stockQuantity - reserved));
                                    item.setUpdatedAt(LocalDateTime.now());
                                    inventoryItemRepository.save(item);
                                    updated++;
                                    logger.debug("Updated inventory for productId={}: {} -> {}", productId, oldStock, stockQuantity);
                                } else {
                                    skipped++;
                                }
                            } else {
                                // Create new inventory item
                                InventoryItem item = new InventoryItem();
                                item.setProductId(productId);
                                item.setWarehouseLocation("Main Warehouse");
                                item.setQuantityOnHand(stockQuantity);
                                item.setQuantityAvailable(stockQuantity);
                                item.setQuantityReserved(0);
                                item.setMinStockLevel(10);
                                item.setReorderPoint(20);
                                item.setLastRestockDate(LocalDateTime.now());
                                item.setCreatedAt(LocalDateTime.now());
                                item.setUpdatedAt(LocalDateTime.now());
                                
                                inventoryItemRepository.save(item);
                                created++;
                                logger.debug("Created inventory for productId={} with stock={}", productId, stockQuantity);
                            }
                        } catch (Exception e) {
                            errors++;
                            logger.error("Error processing product: {}", e.getMessage());
                        }
                    }
                    
                    // Check if there are more pages
                    Object totalPagesObj = response.get("totalPages");
                    int totalPages = totalPagesObj instanceof Number ? ((Number) totalPagesObj).intValue() : 0;
                    page++;
                    
                    if (page >= totalPages) {
                        hasMore = false;
                    }
                    
                } catch (Exception e) {
                    logger.error("Error fetching products from page {}: {}", page, e.getMessage());
                    hasMore = false;
                }
            }
            
            logger.info("✅ Sync completed: total={}, created={}, updated={}, skipped={}, errors={}", 
                totalProducts, created, updated, skipped, errors);
            
            return Map.of(
                "success", true,
                "message", "Sync completed successfully",
                "totalProducts", totalProducts,
                "created", created,
                "updated", updated,
                "skipped", skipped,
                "errors", errors
            );
            
        } catch (Exception e) {
            logger.error("❌ Failed to sync all products: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "totalProducts", totalProducts,
                "created", created,
                "updated", updated,
                "skipped", skipped,
                "errors", errors
            );
        }
    }
    
    // Inner class for inventory status
    public static class InventoryStatus {
        public final int totalQuantity;
        public final int availableQuantity;
        public final int reservedQuantity;
        public final int soldQuantity;
        
        public InventoryStatus(int totalQuantity, int availableQuantity, int reservedQuantity, int soldQuantity) {
            this.totalQuantity = totalQuantity;
            this.availableQuantity = availableQuantity;
            this.reservedQuantity = reservedQuantity;
            this.soldQuantity = soldQuantity;
        }
    }
}
