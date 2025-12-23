package com.example.product.controller;

import com.example.product.entity.Product;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER') or hasRole('MANAGER')")
public class ProductAdminController {

    @Autowired
    private ProductService productService;

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> req) {
        try {
            // Validate minimal required fields
            String name = req.get("name") != null ? String.valueOf(req.get("name")) : null;
            Number priceNum = (req.get("price") instanceof Number) ? (Number) req.get("price") : null;
            
            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Product name is required"));
            }
            if (priceNum == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Product price is required"));
            }

            Product p = new Product();
            p.setName(name);
            p.setPrice(BigDecimal.valueOf(priceNum.doubleValue()));
            
            // Description optional but entity requires not blank & min 10 chars
            String description = req.get("description") != null ? String.valueOf(req.get("description")) : name;
            if (description.length() < 10) {
                description = description + " - High quality product"; // Pad with meaningful text
            }
            p.setDescription(description);
            
            // Ensure SKU exists (entity requires unique & not blank)
            String sku = req.get("sku") != null && !String.valueOf(req.get("sku")).isBlank() 
                ? String.valueOf(req.get("sku")) 
                : ("SKU-" + name.replaceAll("[^A-Za-z0-9]", "").toUpperCase() + "-" + System.currentTimeMillis());
            p.setSku(sku);

            if (req.containsKey("stockQuantity") && req.get("stockQuantity") instanceof Number) 
                p.setStockQuantity(((Number) req.get("stockQuantity")).intValue());
            
            if (req.containsKey("imageUrl")) 
                p.setImageUrl((String) req.get("imageUrl"));
            
            if (req.containsKey("isOnSale") && req.get("isOnSale") instanceof Boolean) {
                Boolean isOnSale = (Boolean) req.get("isOnSale");
                p.setIsOnSale(isOnSale);
                // Khi tắt sale, xóa salePrice
                if (!isOnSale) {
                    p.setSalePrice(null);
                }
            }
            
            if (req.containsKey("salePrice") && req.get("salePrice") instanceof Number) 
                p.setSalePrice(BigDecimal.valueOf(((Number) req.get("salePrice")).doubleValue()));
            
            // Safe Date Parsing with seconds handling
            if (req.containsKey("saleStartAt") && req.get("saleStartAt") instanceof String) {
                String dateStr = (String) req.get("saleStartAt");
                if (!dateStr.isBlank()) {
                    if (dateStr.length() == 16) dateStr += ":00"; // Append seconds if missing
                    try {
                        p.setSaleStartAt(LocalDateTime.parse(dateStr));
                    } catch (Exception e) {
                        System.err.println("Error parsing saleStartAt: " + dateStr + " - " + e.getMessage());
                    }
                }
            }
            
            if (req.containsKey("saleEndAt") && req.get("saleEndAt") instanceof String) {
                String dateStr = (String) req.get("saleEndAt");
                if (!dateStr.isBlank()) {
                    if (dateStr.length() == 16) dateStr += ":00"; // Append seconds if missing
                    try {
                        p.setSaleEndAt(LocalDateTime.parse(dateStr));
                    } catch (Exception e) {
                        System.err.println("Error parsing saleEndAt: " + dateStr + " - " + e.getMessage());
                    }
                }
            }

            // Map category/brand: prefer IDs if provided; otherwise resolve by name
            if (req.containsKey("categoryId") && req.get("categoryId") instanceof Number) {
                p.setCategoryId(((Number) req.get("categoryId")).longValue());
            }
            if (req.containsKey("brandId") && req.get("brandId") instanceof Number) {
                p.setBrandId(((Number) req.get("brandId")).longValue());
            }
            String categoryName = req.get("category") != null ? String.valueOf(req.get("category")) : null;
            String brandName = req.get("brand") != null ? String.valueOf(req.get("brand")) : null;
            productService.resolveCategoryAndBrand(p, categoryName, brandName);

            Product saved = productService.save(p);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("id", saved.getId());
            res.put("data", saved);
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "success", false, 
                "error", "Failed to create product: " + e.getMessage()
            ));
        }
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Product> result = productService.findAll(PageRequest.of(page, size));
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("content", result.getContent());
        res.put("totalElements", result.getTotalElements());
        res.put("totalPages", result.getTotalPages());
        res.put("page", result.getNumber());
        return ResponseEntity.ok(res);
    }

    /**
     * Admin: list ALL variants for a product (including inactive).
     * Note: public endpoint `/api/products/{id}/variants` only returns active variants.
     */
    @GetMapping("/{id}/variants")
    public ResponseEntity<Map<String, Object>> listVariants(@PathVariable String id) {
        try {
            return productService.findById(id)
                .map(p -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("productId", id);
                    res.put("variants", productService.getAllProductVariants(id));
                    return ResponseEntity.ok(res);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> req) {
        try {
            return productService.findById(id)
                    .map(p -> {
                        if (req.containsKey("name")) p.setName((String) req.get("name"));
                        
                        if (req.containsKey("description")) {
                            String desc = (String) req.get("description");
                            if (desc != null && desc.length() < 10) desc += " - High quality product";
                            p.setDescription(desc);
                        }
                        
                        if (req.containsKey("price") && req.get("price") instanceof Number) 
                            p.setPrice(BigDecimal.valueOf(((Number) req.get("price")).doubleValue()));
                        
                        if (req.containsKey("stockQuantity") && req.get("stockQuantity") instanceof Number) 
                            p.setStockQuantity(((Number) req.get("stockQuantity")).intValue());
                        
                        if (req.containsKey("imageUrl")) p.setImageUrl((String) req.get("imageUrl"));
                        
                        if (req.containsKey("isOnSale") && req.get("isOnSale") instanceof Boolean) {
                            Boolean isOnSale = (Boolean) req.get("isOnSale");
                            p.setIsOnSale(isOnSale);
                            // Khi tắt sale, xóa salePrice
                            if (!isOnSale) {
                                p.setSalePrice(null);
                                p.setSaleStartAt(null);
                                p.setSaleEndAt(null);
                            }
                        }
                        
                        if (req.containsKey("salePrice")) {
                            if (req.get("salePrice") instanceof Number) {
                                p.setSalePrice(BigDecimal.valueOf(((Number) req.get("salePrice")).doubleValue()));
                            } else if (req.get("salePrice") == null) {
                                p.setSalePrice(null);
                            }
                        }
                        
                        // Safe Date Parsing with seconds handling
                        if (req.containsKey("saleStartAt") && req.get("saleStartAt") instanceof String) {
                            String dateStr = (String) req.get("saleStartAt");
                            if (dateStr != null && !dateStr.isBlank()) {
                                if (dateStr.length() == 16) dateStr += ":00";
                                try {
                                    p.setSaleStartAt(LocalDateTime.parse(dateStr));
                                } catch (Exception e) {
                                    System.err.println("Error parsing saleStartAt: " + dateStr + " - " + e.getMessage());
                                }
                            } else {
                                p.setSaleStartAt(null);
                            }
                        }
                        
                        if (req.containsKey("saleEndAt") && req.get("saleEndAt") instanceof String) {
                            String dateStr = (String) req.get("saleEndAt");
                            if (dateStr != null && !dateStr.isBlank()) {
                                if (dateStr.length() == 16) dateStr += ":00";
                                try {
                                    p.setSaleEndAt(LocalDateTime.parse(dateStr));
                                } catch (Exception e) {
                                    System.err.println("Error parsing saleEndAt: " + dateStr + " - " + e.getMessage());
                                }
                            } else {
                                p.setSaleEndAt(null);
                            }
                        }
                        
                        // Map category/brand on update
                        if (req.containsKey("categoryId") && req.get("categoryId") instanceof Number) {
                            p.setCategoryId(((Number) req.get("categoryId")).longValue());
                        }
                        if (req.containsKey("brandId") && req.get("brandId") instanceof Number) {
                            p.setBrandId(((Number) req.get("brandId")).longValue());
                        }
                        String categoryName = req.get("category") != null ? String.valueOf(req.get("category")) : null;
                        String brandName = req.get("brand") != null ? String.valueOf(req.get("brand")) : null;
                        productService.resolveCategoryAndBrand(p, categoryName, brandName);

                        productService.save(p);
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        return ResponseEntity.ok(res);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        return productService.findById(id)
                .map(p -> {
                    productService.deleteById(id);
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    return ResponseEntity.ok(res);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/fix-stock")
    public ResponseEntity<Map<String, Object>> fixStock() {
        int updated = 0;
        for (Product p : productService.findAll(PageRequest.of(0, Integer.MAX_VALUE)).getContent()) {
            if (p.getStockQuantity() == null || p.getStockQuantity() <= 0) {
                p.setStockQuantity(20);
                productService.save(p);
                updated++;
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("updated", updated);
        return ResponseEntity.ok(res);
    }

    /**
     * Batch update product images from Unsplash
     * Updates all products that don't have images (null or empty imageUrl)
     */
    @PostMapping("/batch-update-images")
    public ResponseEntity<Map<String, Object>> batchUpdateImages() {
        try {
            int updated = productService.batchUpdateProductImages();
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("updated", updated);
            res.put("message", "Successfully updated " + updated + " product images from Unsplash");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }
}
