package com.example.product.service;

import com.example.product.entity.Product;
import com.example.product.entity.ProductImage;
import com.example.product.entity.ProductVariant;
import com.example.product.entity.Brand;
import com.example.product.entity.Category;
import com.example.product.repository.ProductRepository;
import com.example.product.repository.ProductImageRepository;
import com.example.product.repository.ProductVariantRepository;
import com.example.product.repository.BrandRepository;
import com.example.product.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final Map<String, String> SORT_FIELD_MAPPING = Map.ofEntries(
        Map.entry("id", "id"),
        Map.entry("name", "name"),
        Map.entry("price", "price"),
        Map.entry("salePrice", "salePrice"),
        Map.entry("createdAt", "createdAt"),
        Map.entry("updatedAt", "updatedAt"),
        Map.entry("featured", "isFeatured"),
        Map.entry("isFeatured", "isFeatured"),
        Map.entry("onSale", "isOnSale"),
        Map.entry("isOnSale", "isOnSale"),
        Map.entry("rating", "averageRating"),
        Map.entry("averageRating", "averageRating"),
        Map.entry("reviews", "reviewCount"),
        Map.entry("reviewCount", "reviewCount"),
        Map.entry("popularity", "purchaseCount")
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.order.base-url:http://localhost:8087}")
    private String orderServiceUrl;
    
    @Value("${services.inventory.base-url:http://localhost:8093}")
    private String inventoryServiceUrl;
    
    @Value("${services.category.base-url:http://localhost:8089}")
    private String categoryServiceUrl;
    
    @Value("${services.brand.base-url:http://localhost:8090}")
    private String brandServiceUrl;

    @Cacheable(value = "products", key = "#id")
    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public Page<Product> findAllActiveWithFilters(int page, int size, String search, String category, String brand, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, String sort) {
        Pageable pageable = createPageable(page, size, sort);

        String searchTerm = search != null ? search.trim() : null;
        String categoryTerm = category != null ? category.trim() : null;
        String brandTerm = brand != null ? brand.trim() : null;

        boolean hasSearch = searchTerm != null && !searchTerm.isBlank();
        boolean hasCategory = categoryTerm != null && !categoryTerm.isBlank();
        boolean hasBrand = brandTerm != null && !brandTerm.isBlank();
        boolean hasPriceRange = minPrice != null && maxPrice != null;
        
        // Full filter: search + category + brand + price range
        if (hasSearch && hasCategory && hasBrand && hasPriceRange) {
            return productRepository.findActiveProductsWithAllFilters(
                searchTerm, categoryTerm, brandTerm, minPrice, maxPrice, pageable);
        }
        // Search + category + brand
        else if (hasSearch && hasCategory && hasBrand) {
            return productRepository.findActiveProductsBySearchCategoryAndBrand(
                searchTerm, categoryTerm, brandTerm, pageable);
        }
        // Search + category + price range
        else if (hasSearch && hasCategory && hasPriceRange) {
            // Need to add this query or use existing and filter in memory (not ideal)
            // For now, use search+category and filter price in service
            Page<Product> products = productRepository.findActiveProductsBySearchAndCategory(
                searchTerm, categoryTerm, pageable);
            return filterByPriceRange(products, minPrice, maxPrice, pageable);
        }
        // Search + brand + price range
        else if (hasSearch && hasBrand && hasPriceRange) {
            Page<Product> products = productRepository.findActiveProductsBySearchAndBrand(
                searchTerm, brandTerm, pageable);
            return filterByPriceRange(products, minPrice, maxPrice, pageable);
        }
        // Search + price range
        else if (hasSearch && hasPriceRange) {
            return productRepository.findActiveProductsBySearchAndPriceRange(
                searchTerm, minPrice, maxPrice, pageable);
        }
        // Search + category
        else if (hasSearch && hasCategory) {
            return productRepository.findActiveProductsBySearchAndCategory(
                searchTerm, categoryTerm, pageable);
        }
        // Search + brand
        else if (hasSearch && hasBrand) {
            return productRepository.findActiveProductsBySearchAndBrand(
                searchTerm, brandTerm, pageable);
        }
        // Search only
        else if (hasSearch) {
            return productRepository.findActiveProductsBySearch(searchTerm, pageable);
        }
        // Category + brand
        else if (hasCategory && hasBrand) {
            return productRepository.findActiveProductsByCategoryAndBrand(
                categoryTerm, brandTerm, pageable);
        }
        // Category only
        else if (hasCategory) {
            return productRepository.findActiveProductsByCategory(categoryTerm, pageable);
        }
        // Brand only
        else if (hasBrand) {
            return productRepository.findActiveProductsByBrand(brandTerm, pageable);
        }
        // No filters
        else {
            Page<Product> allProducts = productRepository.findByIsActiveTrueAndIsDeletedFalse(pageable);
            if (hasPriceRange) {
                return filterByPriceRange(allProducts, minPrice, maxPrice, pageable);
            }
            return allProducts;
        }
    }
    
    /**
     * Helper method to filter products by price range
     */
    private Page<Product> filterByPriceRange(Page<Product> products, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, Pageable pageable) {
        List<Product> filtered = products.getContent().stream()
            .filter(p -> {
                java.math.BigDecimal price = p.getSalePrice() != null ? p.getSalePrice() : p.getPrice();
                return price != null && price.compareTo(minPrice) >= 0 && price.compareTo(maxPrice) <= 0;
            })
            .collect(java.util.stream.Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Product> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
        
        return new PageImpl<>(
            pageContent, pageable, filtered.size());
    }

    public Page<Product> findFeatured(int page, int size, String sort) {
        Pageable pageable = createPageable(page, size, sort);
        return productRepository.findFeatured(pageable);
    }

    public Page<Product> findOnSale(int page, int size, String sort) {
        Pageable pageable = createPageable(page, size, sort);
        return productRepository.findOnSale(pageable);
    }
    
    /**
     * Find on-sale products with additional filters
     */
    public Page<Product> findOnSaleWithFilters(int page, int size, String search, String category, String brand, String sort, String saleTimeSlot) {
        Pageable pageable = createPageable(page, size, sort != null ? sort : "salePrice,asc");
        
        String searchTerm = search != null ? search.trim() : null;
        String categoryTerm = category != null ? category.trim() : null;
        String brandTerm = brand != null ? brand.trim() : null;
        
        boolean hasSearch = searchTerm != null && !searchTerm.isBlank();
        boolean hasCategory = categoryTerm != null && !categoryTerm.isBlank();
        boolean hasBrand = brandTerm != null && !brandTerm.isBlank();
        final String lowerSearch = searchTerm == null ? "" : searchTerm.toLowerCase();
        
        // If a flash-sale slot is provided, filter by that window (2 hours per slot)
        LocalDateTime slotStart = null;
        LocalDateTime slotEnd = null;
        if (saleTimeSlot != null && !saleTimeSlot.isBlank()) {
            try {
                slotStart = LocalDateTime.parse(saleTimeSlot.trim());
                slotEnd = slotStart.plusHours(2);
            } catch (Exception _e) {
                // Ignore parsing errors; fall back to CURRENT_TIMESTAMP-based on-sale query
                slotStart = null;
                slotEnd = null;
            }
        }

        Page<Product> onSaleProducts;
        if (slotStart != null && slotEnd != null) {
            // Fetch a larger window page first, then paginate after in-memory filters to avoid empty pages
            Pageable wide = org.springframework.data.domain.PageRequest.of(0, 2000, pageable.getSort());
            onSaleProducts = productRepository.findOnSaleInWindow(slotStart, slotEnd, wide);
        } else {
            onSaleProducts = productRepository.findOnSale(org.springframework.data.domain.PageRequest.of(0, 2000, pageable.getSort()));
        }
        
        // Apply additional filters in memory (for simplicity)
        List<Product> filtered = onSaleProducts.getContent().stream()
            .filter(p -> {
                // Filter by search term
                if (hasSearch) {
                    boolean matchesName = p.getName() != null && p.getName().toLowerCase().contains(lowerSearch);
                    boolean matchesDesc = p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerSearch);
                    if (!matchesName && !matchesDesc) return false;
                }
                return true;
            })
            .filter(p -> {
                // Filter by category (by categoryId)
                if (hasCategory) {
                    try {
                        Long catId = Long.parseLong(categoryTerm);
                        return p.getCategoryId() != null && p.getCategoryId().equals(catId);
                    } catch (NumberFormatException e) {
                        // If category is a name, resolve to ID and compare
                        try {
                            Category c = categoryRepository.findByNameIgnoreCase(categoryTerm).orElse(null);
                            if (c == null || c.getId() == null) return true;
                            return p.getCategoryId() != null && p.getCategoryId().equals(c.getId());
                        } catch (Exception _ignored) {
                            return true;
                        }
                    }
                }
                return true;
            })
            .filter(p -> {
                // Filter by brand (by brandId)
                if (hasBrand) {
                    try {
                        Long bId = Long.parseLong(brandTerm);
                        return p.getBrandId() != null && p.getBrandId().equals(bId);
                    } catch (NumberFormatException e) {
                        // If brand is a name, resolve to ID and compare
                        try {
                            Brand b = brandRepository.findByNameIgnoreCase(brandTerm).orElse(null);
                            if (b == null || b.getId() == null) return true;
                            return p.getBrandId() != null && p.getBrandId().equals(b.getId());
                        } catch (Exception _ignored) {
                            return true;
                        }
                    }
                }
                return true;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Create paginated result AFTER filtering
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Product> pageContent = start < filtered.size() ? filtered.subList(start, end) : List.of();
        
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }
    
    public Page<Product> findBestSelling(int page, int size) {
        try {
            // Try to fetch best-selling from order-service (real sales data)
            String url = orderServiceUrl + "/api/orders/top-products?limit=" + size;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("X-Internal-Request", "true");
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef =
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
            org.springframework.http.ResponseEntity<Map<String, Object>> resp =
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, typeRef);

            Map<String, Object> body = resp.getBody();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> topProducts = body != null && body.get("products") instanceof List
                ? (List<Map<String, Object>>) body.get("products")
                : java.util.List.of();

            // Preserve order returned by order-service
            List<String> productIds = topProducts.stream()
                .map(m -> m.get("productId"))
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .toList();

            if (productIds.isEmpty()) {
                return Page.empty();
            }

            // Fetch product details and keep ordering
            Map<String, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

            List<Product> ordered = new java.util.ArrayList<>();
            for (String pid : productIds) {
                Product p = productMap.get(pid);
                if (p != null) {
                    ordered.add(p);
                }
            }

            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), ordered.size());
            List<Product> content = start < ordered.size() ? ordered.subList(start, end) : List.of();

            return new PageImpl<>(content, pageable, ordered.size());
        } catch (Exception e) {
            logger.warn("Fallback to local best-selling due to order-service error: {}", e.getMessage());
            // Fallback: sort by purchaseCount
            Pageable fallbackPageable = PageRequest.of(page, size, 
                Sort.by(Sort.Order.desc("purchaseCount"), Sort.Order.desc("averageRating")));
            return productRepository.findByIsActiveTrueAndIsDeletedFalse(fallbackPageable);
        }
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findByIsActiveTrueAndIsDeletedFalse(pageable);
    }

    /**
     * Save product and sync inventory
     * ĐỒNG BỘ DỮ LIỆU: Khi admin tạo/update product → tự động sync inventory
     */
    @Transactional
    @CacheEvict(value = "products", key = "#product.id", condition = "#product.id != null")
    public Product save(Product product) {
        boolean isNewProduct = (product.getId() == null);
        
        // Save product to DB
        Product savedProduct = productRepository.save(product);
        
        // SYNC WITH INVENTORY SERVICE
        if (isNewProduct) {
            // Tạo mới sản phẩm → Tạo inventory record
            syncInventoryForNewProduct(savedProduct);
        } else {
            // Update sản phẩm → Sync stock với inventory
            syncInventoryStock(savedProduct);
        }
        
        return savedProduct;
    }
    
    /**
     * Sync inventory khi tạo sản phẩm mới
     */
    private void syncInventoryForNewProduct(Product product) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/create";
            
            Map<String, Object> inventoryData = new HashMap<>();
            inventoryData.put("productId", product.getId());
            inventoryData.put("productName", product.getName());
            inventoryData.put("initialStock", product.getStockQuantity() != null ? product.getStockQuantity() : 0);
            inventoryData.put("warehouseLocation", "Main Warehouse");
            
            restTemplate.postForEntity(url, inventoryData, Map.class);
            
            logger.info("✅ Synced inventory for new product: productId={}, initialStock={}", 
                product.getId(), product.getStockQuantity());
        } catch (Exception e) {
            logger.error("❌ Failed to sync inventory for new product {}: {}", product.getId(), e.getMessage());
            // Don't fail product creation if inventory sync fails
        }
    }
    
    /**
     * Sync stock khi update sản phẩm
     */
    private void syncInventoryStock(Product product) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/sync-stock";
            
            Map<String, Object> stockData = new HashMap<>();
            stockData.put("productId", product.getId());
            stockData.put("stockQuantity", product.getStockQuantity() != null ? product.getStockQuantity() : 0);
            
            restTemplate.postForEntity(url, stockData, Map.class);
            
            logger.info("✅ Synced stock for product: productId={}, stock={}", 
                product.getId(), product.getStockQuantity());
        } catch (Exception e) {
            logger.error("❌ Failed to sync stock for product {}: {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Resolve category/brand IDs when only names are provided
     */
    public void resolveCategoryAndBrand(Product product, String categoryName, String brandName) {
        // Prefer IDs already set
        if (product.getCategoryId() == null && categoryName != null && !categoryName.isBlank()) {
            Long cid = fetchCategoryIdByName(categoryName.trim());
            if (cid != null) {
                product.setCategoryId(cid);
            }
        }
        if (product.getBrandId() == null && brandName != null && !brandName.isBlank()) {
            Long bid = fetchBrandIdByName(brandName.trim());
            if (bid != null) {
                product.setBrandId(bid);
            }
        }
    }

    private Long fetchCategoryIdByName(String name) {
        try {
            String url = categoryServiceUrl + "/api/categories/search?name=" + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null) {
                Object id = resp.get("id");
                if (id instanceof Number) return ((Number) id).longValue();
                if (id != null) {
                    try { return Long.parseLong(id.toString()); } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve category '{}' : {}", name, e.getMessage());
        }
        return null;
    }

    private Long fetchBrandIdByName(String name) {
        try {
            String url = brandServiceUrl + "/api/brands/search?name=" + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null) {
                Object id = resp.get("id");
                if (id instanceof Number) return ((Number) id).longValue();
                if (id != null) {
                    try { return Long.parseLong(id.toString()); } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve brand '{}' : {}", name, e.getMessage());
        }
        return null;
    }

    @CacheEvict(value = "products", key = "#id")
    public void deleteById(String id) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isPresent()) {
            Product p = opt.get();
            p.setIsDeleted(true);
            p.setIsActive(false);
            productRepository.save(p);
        }
    }

    /**
     * Update stock for all products
     */
    public int updateAllStock(Integer stockQuantity) {
        return productRepository.updateAllStock(stockQuantity);
    }
    
    /**
     * Update stock for a specific product
     */
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public boolean updateProductStock(String productId, Integer stockQuantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setStockQuantity(stockQuantity);
            productRepository.save(product);
            return true;
        }
        return false;
    }
    
    /**
     * Get variants for a product
     */
    public List<ProductVariant> getProductVariants(String productId) {
        return productVariantRepository.findByProductIdAndIsActiveTrue(productId);
    }

    /**
     * Get ALL variants for a product (including inactive) - for admin management
     */
    public List<ProductVariant> getAllProductVariants(String productId) {
        return productVariantRepository.findByProductId(productId);
    }
    
    /**
     * Get default variant for a product
     */
    public Optional<ProductVariant> getDefaultVariant(String productId) {
        return productVariantRepository.findDefaultVariantByProductId(productId);
    }
    
    /**
     * Get variant by ID
     */
    public Optional<ProductVariant> getVariantById(Long variantId) {
        return productVariantRepository.findById(variantId);
    }
    
    /**
     * Create or update product variant
     */
    @Transactional
    public ProductVariant saveVariant(ProductVariant variant) {
        return productVariantRepository.save(variant);
    }
    
    /**
     * Delete product variant
     */
    @Transactional
    public void deleteVariant(Long variantId) {
        productVariantRepository.deleteById(variantId);
    }
    
    /**
     * Calculate total stock from variants
     */
    public Integer calculateTotalStockFromVariants(String productId) {
        Integer totalStock = productVariantRepository.getTotalStockByProductId(productId);
        return totalStock != null ? totalStock : 0;
    }

    /**
     * Increment view count for a product
     */
    @Transactional
    public void incrementViewCount(String productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Long currentViewCount = product.getViewCount() != null ? product.getViewCount() : 0L;
            product.setViewCount(currentViewCount + 1);
            productRepository.save(product);
        }
    }

    /**
     * Get related products by category
     */
    public List<Product> getRelatedProducts(String productId, int limit) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Page<Product> relatedProducts = productRepository.findRelatedProductsByCategory(
                product.getCategoryId(),
                productId,
                PageRequest.of(0, limit)
            );
            return relatedProducts.getContent();
        }
        return List.of();
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        String[] parts = sort.split(",");
        String field = resolveSortField(parts[0]);
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(dir, field));
    }

    private String resolveSortField(String rawField) {
        if (rawField == null || rawField.isBlank()) {
            return DEFAULT_SORT_FIELD;
        }
        String normalized = rawField.trim();
        return SORT_FIELD_MAPPING.getOrDefault(normalized, DEFAULT_SORT_FIELD);
    }
    
    /**
     * Save product images to database
     * If replaceExisting is true, deletes all existing images before inserting new ones
     */
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public List<ProductImage> saveProductImages(String productId, List<Map<String, Object>> imageDataList) {
        return saveProductImages(productId, imageDataList, false);
    }
    
    /**
     * Save product images to database with replace option
     * @param productId Product ID
     * @param imageDataList List of image data to save
     * @param replaceExisting If true, deletes all existing images before inserting new ones
     */
    @Transactional
    @CacheEvict(value = "products", key = "#productId")
    public List<ProductImage> saveProductImages(String productId, List<Map<String, Object>> imageDataList, boolean replaceExisting) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new RuntimeException("Product not found: " + productId);
        }
        
        Product product = productOpt.get();
        
        // If replaceExisting is true, delete all existing images first
        if (replaceExisting) {
            List<ProductImage> existingImages = productImageRepository.findByProductIdOrderByDisplayOrderAscIsPrimaryDesc(productId);
            logger.info("🗑️ Replacing {} existing images for product: {}", existingImages.size(), productId);
            
            // Delete all existing images from database
            productImageRepository.deleteAllByProductId(productId);
            logger.info("✅ Deleted {} existing images from database", existingImages.size());
        }
        
        List<ProductImage> savedImages = new java.util.ArrayList<>();
        
        // Unset all primary images first if any new image is primary
        boolean hasPrimary = imageDataList.stream()
            .anyMatch(img -> Boolean.TRUE.equals(img.get("isPrimary")));
        if (hasPrimary) {
            productImageRepository.unsetAllPrimaryByProductId(productId);
        }
        
        int displayOrder = 0;
        for (Map<String, Object> imageData : imageDataList) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl((String) imageData.get("imageUrl"));
            image.setIsPrimary(Boolean.TRUE.equals(imageData.get("isPrimary")));
            image.setDisplayOrder(displayOrder++);
            image.setAltText((String) imageData.getOrDefault("altText", ""));
            
            savedImages.add(productImageRepository.save(image));
        }
        
        // Update product's main image_url if first image is primary
        if (!savedImages.isEmpty() && savedImages.get(0).getIsPrimary()) {
            product.setImageUrl(savedImages.get(0).getImageUrl());
            productRepository.save(product);
        }
        
        logger.info("✅ Saved {} images for product: {}", savedImages.size(), productId);
        return savedImages;
    }
    
    /**
     * Get product images
     */
    public List<ProductImage> getProductImages(String productId) {
        return productImageRepository.findByProductIdOrderByDisplayOrderAscIsPrimaryDesc(productId);
    }
    
    /**
     * Delete product image
     */
    @Transactional
    public boolean deleteProductImage(Long imageId) {
        Optional<ProductImage> imageOpt = productImageRepository.findById(imageId);
        if (imageOpt.isPresent()) {
            productImageRepository.deleteById(imageId);
            return true;
        }
        return false;
    }
    
    /**
     * Generate Unsplash image URL based on product category/name
     * Uses Unsplash Source API (free, no API key needed)
     * Format: https://source.unsplash.com/800x600/?{keyword}
     */
    public String generateUnsplashImageUrl(String categoryName, String productName) {
        // Map categories to search terms for better image matching
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("Laptop", "laptop");
        categoryMap.put("Laptops", "laptop");
        categoryMap.put("Phones", "smartphone");
        categoryMap.put("Phone", "smartphone");
        categoryMap.put("Tablet", "tablet");
        categoryMap.put("Headphones", "headphones");
        categoryMap.put("Smartwatch", "smartwatch");
        categoryMap.put("Camera", "camera");
        categoryMap.put("Gaming Console", "gaming");
        categoryMap.put("TV", "tv");
        categoryMap.put("Smart Home", "smart-home");
        categoryMap.put("Drone", "drone");
        categoryMap.put("Gaming Accessories", "gaming-accessories");
        categoryMap.put("Computer Components", "computer-hardware");
        categoryMap.put("Wearables", "wearables");
        categoryMap.put("Audio Equipment", "audio");
        categoryMap.put("Storage Devices", "storage");
        
        String searchTerm = categoryMap.getOrDefault(categoryName, "technology");
        
        // Use Unsplash Source API - free, no API key needed
        // Format: https://source.unsplash.com/800x600/?{keyword}
        // Add random seed based on product name for variety
        int seed = productName != null ? Math.abs(productName.hashCode()) : (int)(System.currentTimeMillis() % 10000);
        return String.format("https://source.unsplash.com/800x600/?%s&sig=%d", searchTerm, seed);
    }
    
    /**
     * Get category name from category service
     */
    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "technology";
        }
        
        try {
            String categoryUrl = categoryServiceUrl + "/api/categories/" + categoryId;
            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
            org.springframework.http.ResponseEntity<Map<String, Object>> response = 
                restTemplate.exchange(categoryUrl, org.springframework.http.HttpMethod.GET, null, typeRef);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("name")) {
                return (String) body.get("name");
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch category name for categoryId {}: {}", categoryId, e.getMessage());
        }
        
        return "technology"; // Default fallback
    }
    
    /**
     * Batch update product images from Unsplash
     * Updates products that don't have images or have null/empty imageUrl
     */
    @Transactional
    public int batchUpdateProductImages() {
        List<Product> products = productRepository.findAll();
        int updated = 0;
        
        for (Product product : products) {
            // Only update if imageUrl is null or empty
            if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
                try {
                    // Get category name from category service
                    String categoryName = getCategoryName(product.getCategoryId());
                    
                    // Generate Unsplash URL
                    String imageUrl = generateUnsplashImageUrl(categoryName, product.getName());
                    product.setImageUrl(imageUrl);
                    productRepository.save(product);
                    updated++;
                    logger.info("Updated image for product: {} - {} (category: {})", 
                        product.getId(), product.getName(), categoryName);
                } catch (Exception e) {
                    logger.warn("Failed to update image for product {}: {}", product.getId(), e.getMessage());
                }
            }
        }
        
        logger.info("✅ Batch updated {} product images", updated);
        return updated;
    }
}