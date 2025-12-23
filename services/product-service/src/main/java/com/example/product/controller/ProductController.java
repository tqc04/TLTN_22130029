package com.example.product.controller;

import com.example.product.dto.ProductDTO;
import com.example.product.entity.Product;
import com.example.product.entity.ProductImage;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private com.example.product.repository.BrandRepository brandRepository;

    @Autowired
    private com.example.product.repository.CategoryRepository categoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("")
    public ResponseEntity<Page<ProductDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "id,asc") String sort,
            @RequestParam(required = false) String onSale,
            @RequestParam(required = false) String saleTimeSlot) {
        
        // If onSale filter is requested, use the on-sale endpoint logic
        if ("true".equalsIgnoreCase(onSale)) {
            Page<Product> products = productService.findOnSaleWithFilters(page, size, search, category, brand, sort, saleTimeSlot);
            return ResponseEntity.ok(enrichListDtos(products));
        }
        
        Page<Product> products = productService.findAllActiveWithFilters(page, size, search, category, brand, minPrice, maxPrice, sort);
        return ResponseEntity.ok(enrichListDtos(products));
    }

    /**
     * Enrich list DTOs with brand/category names (avoid nulls in list pages).
     * Uses local product-service tables `brands`/`categories` for fast lookup.
     */
    private Page<ProductDTO> enrichListDtos(Page<Product> products) {
        List<Product> content = products.getContent();
        if (content == null || content.isEmpty()) {
            return products.map(ProductDTO::from);
        }

        Set<Long> brandIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (Product p : content) {
            if (p.getBrandId() != null) brandIds.add(p.getBrandId());
            if (p.getCategoryId() != null) categoryIds.add(p.getCategoryId());
        }

        Map<Long, String> brandNames = brandRepository.findAllById(brandIds).stream()
            .filter(b -> b.getId() != null)
            .collect(Collectors.toMap(com.example.product.entity.Brand::getId, com.example.product.entity.Brand::getName, (a, b) -> a));
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
            .filter(c -> c.getId() != null)
            .collect(Collectors.toMap(com.example.product.entity.Category::getId, com.example.product.entity.Category::getName, (a, b) -> a));

        List<ProductDTO> dtos = new ArrayList<>(content.size());
        for (Product p : content) {
            ProductDTO dto = ProductDTO.from(p);
            if (p.getBrandId() != null) dto.setBrand(brandNames.get(p.getBrandId()));
            if (p.getCategoryId() != null) dto.setCategory(categoryNames.get(p.getCategoryId()));
            dtos.add(dto);
        }

        return new org.springframework.data.domain.PageImpl<>(dtos, products.getPageable(), products.getTotalElements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getOne(@PathVariable String id) {
        return productService.findById(id)
                .map(product -> {
                    ProductDTO dto = ProductDTO.from(product);

                    // Set category and brand IDs (frontend can fetch names if needed)
                    // For better performance, we'll include IDs and let frontend handle the names
                    dto.setCategoryId(product.getCategoryId());
                    dto.setBrandId(product.getBrandId());

                    // Fetch category/brand names from external services for better UX
                    try {
                        if (product.getCategoryId() != null) {
                            String categoryUrl = "http://localhost:8089/api/categories/" + product.getCategoryId();
                            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef =
                                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
                            org.springframework.http.ResponseEntity<Map<String, Object>> categoryResponse =
                                restTemplate.exchange(categoryUrl, org.springframework.http.HttpMethod.GET, null, typeRef);
                            Map<String, Object> categoryBody = categoryResponse.getBody();
                            if (categoryBody != null && categoryBody.containsKey("name")) {
                                dto.setCategory((String) categoryBody.get("name"));
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fetch category name for product {}: {}", id, e.getMessage());
                    }

                    try {
                        if (product.getBrandId() != null) {
                            String brandUrl = "http://localhost:8090/api/brands/" + product.getBrandId();
                            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef =
                                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
                            org.springframework.http.ResponseEntity<Map<String, Object>> brandResponse =
                                restTemplate.exchange(brandUrl, org.springframework.http.HttpMethod.GET, null, typeRef);
                            Map<String, Object> brandBody = brandResponse.getBody();
                            if (brandBody != null && brandBody.containsKey("name")) {
                                dto.setBrand((String) brandBody.get("name"));
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fetch brand name for product {}: {}", id, e.getMessage());
                    }

                    // Fetch product images and add to DTO
                    try {
                        List<com.example.product.entity.ProductImage> productImages = productService.getProductImages(id);
                        if (productImages != null && !productImages.isEmpty()) {
                            List<Map<String, Object>> imagesList = new ArrayList<>();
                            for (com.example.product.entity.ProductImage img : productImages) {
                                Map<String, Object> imageMap = new HashMap<>();
                                imageMap.put("id", img.getId());
                                imageMap.put("imageUrl", ProductDTO.transformImageUrl(img.getImageUrl()));
                                imageMap.put("isPrimary", img.getIsPrimary() != null && img.getIsPrimary());
                                imageMap.put("displayOrder", img.getDisplayOrder() != null ? img.getDisplayOrder() : 0);
                                imageMap.put("altText", img.getAltText() != null ? img.getAltText() : "");
                                imagesList.add(imageMap);
                            }
                            dto.setImages(imagesList);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fetch images for product {}: {}", id, e.getMessage());
                    }

                    // Increment view count
                    productService.incrementViewCount(id);

                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/featured")
    public ResponseEntity<Page<ProductDTO>> getFeatured(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Page<Product> products = productService.findFeatured(page, size, sort);
        return ResponseEntity.ok(enrichListDtos(products));
    }

    @GetMapping("/on-sale")
    public ResponseEntity<Page<ProductDTO>> getOnSale(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Page<Product> products = productService.findOnSale(page, size, sort);
        return ResponseEntity.ok(enrichListDtos(products));
    }
    
    /**
     * Get best-selling products (sorted by purchaseCount)
     */
    @GetMapping("/best-selling")
    public ResponseEntity<Page<ProductDTO>> getBestSelling(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<Product> products = productService.findBestSelling(page, size);
        return ResponseEntity.ok(enrichListDtos(products));
    }

    /**
     * Flash sale slots for today (every 2 hours)
     */
    @GetMapping("/flash-sale/slots")
    public ResponseEntity<List<String>> getFlashSaleSlots() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.List<String> slots = new java.util.ArrayList<>();
        for (int hour = 10; hour <= 20; hour += 2) {
            java.time.LocalDateTime start = today.atTime(hour, 0);
            slots.add(start.toString());
        }
        return ResponseEntity.ok(slots);
    }

    /**
     * Update stock for all products (admin endpoint)
     */
    @PostMapping("/update-stock")
    public ResponseEntity<Map<String, String>> updateAllStock(@RequestBody Map<String, Integer> request) {
        try {
            Integer stockQuantity = request.get("stockQuantity");
            if (stockQuantity == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "stockQuantity is required"));
            }
            
            int updatedCount = productService.updateAllStock(stockQuantity);
            return ResponseEntity.ok(Map.of(
                "message", "Updated stock for " + updatedCount + " products",
                "stockQuantity", stockQuantity.toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update stock for a specific product (for inventory service)
     */
    @PutMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateProductStock(@PathVariable String id, @RequestBody Map<String, Integer> request) {
        try {
            Integer stockQuantity = request.get("stockQuantity");
            if (stockQuantity == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "stockQuantity is required"));
            }

            boolean updated = productService.updateProductStock(id, stockQuantity);
            if (updated) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "productId", id,
                    "stockQuantity", stockQuantity
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get related products by category
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<java.util.List<ProductDTO>> getRelatedProducts(
            @PathVariable String id,
            @RequestParam(defaultValue = "4") int limit) {
        try {
            java.util.List<Product> relatedProducts = productService.getRelatedProducts(id, limit);
            java.util.List<ProductDTO> relatedProductDTOs = relatedProducts.stream()
                .map(ProductDTO::from)
                .collect(java.util.stream.Collectors.toList());

            // Fetch category and brand names for related products
            for (ProductDTO dto : relatedProductDTOs) {
                try {
                    // Correct URL for category-service (port 8089)
                    String categoryUrl = "http://localhost:8089/api/categories/" + dto.getCategoryId();
                    org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = 
                        new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
                    org.springframework.http.ResponseEntity<Map<String, Object>> categoryResponse = 
                        restTemplate.exchange(categoryUrl, org.springframework.http.HttpMethod.GET, null, typeRef);
                    Map<String, Object> categoryBody = categoryResponse.getBody();
                    if (categoryBody != null && categoryBody.containsKey("name")) {
                        dto.setCategory((String) categoryBody.get("name"));
                    }
                } catch (Exception e) {
                    // Ignore errors for related products
                    logger.warn("Failed to fetch category for product {}: {}", dto.getId(), e.getMessage());
                }

                try {
                    // Correct URL for brand-service (port 8090)
                    String brandUrl = "http://localhost:8090/api/brands/" + dto.getBrandId();
                    org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = 
                        new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
                    org.springframework.http.ResponseEntity<Map<String, Object>> brandResponse = 
                        restTemplate.exchange(brandUrl, org.springframework.http.HttpMethod.GET, null, typeRef);
                    Map<String, Object> brandBody = brandResponse.getBody();
                    if (brandBody != null && brandBody.containsKey("name")) {
                        dto.setBrand((String) brandBody.get("name"));
                    }
                } catch (Exception e) {
                    // Ignore errors for related products
                    logger.warn("Failed to fetch brand for product {}: {}", dto.getId(), e.getMessage());
                }
            }

            return ResponseEntity.ok(relatedProductDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.List.of());
        }
    }

    /**
     * Search products with filters
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "name,asc") String sort) {
        Page<Product> products = productService.findAllActiveWithFilters(page, size, q, category, brand, minPrice, maxPrice, sort);
        return ResponseEntity.ok(products.map(ProductDTO::from));
    }

    /**
     * Autocomplete search - returns limited results for suggestions
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<ProductDTO>> autocomplete(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "5") int limit) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(java.util.List.of());
        }
        // Limit to max 10 for performance
        int maxLimit = Math.min(limit, 10);
        Page<Product> products = productService.findAllActiveWithFilters(0, maxLimit, q, null, null, null, null, "name,asc");
        List<ProductDTO> suggestions = products.getContent().stream()
            .map(ProductDTO::from)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * Save product images (called by admin-service after file upload)
     * This endpoint is called by admin-service after uploading files to disk
     * It saves the image metadata to the database
     */
    @PostMapping("/{productId}/images")
    public ResponseEntity<Map<String, Object>> saveProductImages(
            @PathVariable String productId,
            @RequestBody List<Map<String, Object>> imageDataList,
            @RequestParam(value = "replace", defaultValue = "false") boolean replace) {
        try {
            logger.info("🔵 Received request to save {} images for product: {} (replace: {})", 
                imageDataList.size(), productId, replace);
            logger.info("🔵 Image data received: {}", imageDataList);
            
            List<ProductImage> savedImages = productService.saveProductImages(productId, imageDataList, replace);
            
            logger.info("✅ Successfully saved {} images to database for product: {}", savedImages.size(), productId);
            
            List<Map<String, Object>> imageResponse = new ArrayList<>();
            for (ProductImage img : savedImages) {
                Map<String, Object> imageMap = new HashMap<>();
                imageMap.put("id", img.getId());
                imageMap.put("imageUrl", ProductDTO.transformImageUrl(img.getImageUrl()));
                imageMap.put("isPrimary", img.getIsPrimary() != null && img.getIsPrimary());
                imageMap.put("displayOrder", img.getDisplayOrder() != null ? img.getDisplayOrder() : 0);
                imageMap.put("altText", img.getAltText() != null ? img.getAltText() : "");
                imageResponse.add(imageMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", imageResponse);
            response.put("count", savedImages.size());
            response.put("replaced", replace);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Failed to save product images for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Get product images
     */
    @GetMapping("/{productId}/images")
    public ResponseEntity<Map<String, Object>> getProductImages(@PathVariable String productId) {
        try {
            List<ProductImage> images = productService.getProductImages(productId);
            
            List<Map<String, Object>> imageResponse = new ArrayList<>();
            for (ProductImage img : images) {
                Map<String, Object> imageMap = new HashMap<>();
                imageMap.put("id", img.getId());
                imageMap.put("imageUrl", ProductDTO.transformImageUrl(img.getImageUrl()));
                imageMap.put("isPrimary", img.getIsPrimary() != null && img.getIsPrimary());
                imageMap.put("displayOrder", img.getDisplayOrder() != null ? img.getDisplayOrder() : 0);
                imageMap.put("altText", img.getAltText() != null ? img.getAltText() : "");
                imageResponse.add(imageMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", imageResponse);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get product images: {}", e.getMessage(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}


