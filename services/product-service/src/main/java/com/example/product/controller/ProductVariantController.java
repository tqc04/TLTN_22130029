package com.example.product.controller;

import com.example.product.entity.ProductVariant;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductVariantController {

    @Autowired
    private ProductService productService;

    /**
     * Get variants for a product (public)
     */
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariant>> getProductVariants(@PathVariable String productId) {
        List<ProductVariant> variants = productService.getProductVariants(productId);
        return ResponseEntity.ok(variants);
    }

    /**
     * Get default variant for a product (public)
     */
    @GetMapping("/{productId}/variants/default")
    public ResponseEntity<ProductVariant> getDefaultVariant(@PathVariable String productId) {
        Optional<ProductVariant> variant = productService.getDefaultVariant(productId);
        return variant.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get variant by ID (public) - for cart service
     */
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ProductVariant> getVariantById(@PathVariable Long variantId) {
        Optional<ProductVariant> variant = productService.getVariantById(variantId);
        return variant.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create variant for a product (admin only)
     */
    @PostMapping("/{productId}/variants")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> createVariant(@PathVariable String productId, @RequestBody Map<String, Object> req) {
        try {
            ProductVariant variant = new ProductVariant();
            
            // Set basic properties
            if (req.containsKey("variantName")) variant.setVariantName((String) req.get("variantName"));
            if (req.containsKey("sku")) variant.setSku((String) req.get("sku"));
            if (req.containsKey("color")) variant.setColor((String) req.get("color"));
            if (req.containsKey("size")) variant.setSize((String) req.get("size"));
            if (req.containsKey("material")) variant.setMaterial((String) req.get("material"));
            if (req.containsKey("style")) variant.setStyle((String) req.get("style"));
            if (req.containsKey("barcode")) variant.setBarcode((String) req.get("barcode"));
            if (req.containsKey("imageUrl")) variant.setImageUrl((String) req.get("imageUrl"));
            if (req.containsKey("dimensions")) variant.setDimensions((String) req.get("dimensions"));
            if (req.containsKey("weightKg") && req.get("weightKg") instanceof Number) {
                variant.setWeightKg(((Number) req.get("weightKg")).doubleValue());
            }
            
            // Set pricing
            if (req.containsKey("price") && req.get("price") instanceof Number) {
                variant.setPrice(BigDecimal.valueOf(((Number) req.get("price")).doubleValue()));
            }
            if (req.containsKey("costPrice") && req.get("costPrice") instanceof Number) {
                variant.setCostPrice(BigDecimal.valueOf(((Number) req.get("costPrice")).doubleValue()));
            }
            
            // Set stock
            if (req.containsKey("stockQuantity") && req.get("stockQuantity") instanceof Number) {
                variant.setStockQuantity(((Number) req.get("stockQuantity")).intValue());
            }
            
            // Set flags
            if (req.containsKey("isActive") && req.get("isActive") instanceof Boolean) {
                variant.setActive((Boolean) req.get("isActive"));
            }
            if (req.containsKey("isDefault") && req.get("isDefault") instanceof Boolean) {
                variant.setDefault((Boolean) req.get("isDefault"));
            }
            
            // Set product (you'll need to fetch this from productService)
            var productOpt = productService.findById(productId);
            if (productOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            variant.setProduct(productOpt.get());
            
            ProductVariant saved = productService.saveVariant(variant);
            
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("variantId", saved.getId());
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    /**
     * Update variant (admin only)
     */
    @PutMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> updateVariant(@PathVariable Long variantId, @RequestBody Map<String, Object> req) {
        try {
            // Find existing variant
            Optional<ProductVariant> variantOpt = productService.getVariantById(variantId);
            if (variantOpt.isEmpty()) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("error", "Variant not found with id: " + variantId);
                return ResponseEntity.notFound().build();
            }
            
            ProductVariant variant = variantOpt.get();
            
            // Update basic properties (only if provided in request)
            if (req.containsKey("variantName")) {
                variant.setVariantName((String) req.get("variantName"));
            }
            if (req.containsKey("sku")) {
                variant.setSku((String) req.get("sku"));
            }
            if (req.containsKey("color")) {
                variant.setColor((String) req.get("color"));
            }
            if (req.containsKey("size")) {
                variant.setSize((String) req.get("size"));
            }
            if (req.containsKey("material")) {
                variant.setMaterial((String) req.get("material"));
            }
            if (req.containsKey("style")) {
                variant.setStyle((String) req.get("style"));
            }
            if (req.containsKey("barcode")) {
                variant.setBarcode((String) req.get("barcode"));
            }
            if (req.containsKey("imageUrl")) {
                variant.setImageUrl((String) req.get("imageUrl"));
            }
            if (req.containsKey("dimensions")) {
                variant.setDimensions((String) req.get("dimensions"));
            }
            if (req.containsKey("weightKg") && req.get("weightKg") != null) {
                if (req.get("weightKg") instanceof Number) {
                    variant.setWeightKg(((Number) req.get("weightKg")).doubleValue());
                }
            }
            
            // Update pricing (only if provided)
            if (req.containsKey("price") && req.get("price") != null) {
                if (req.get("price") instanceof Number) {
                    variant.setPrice(BigDecimal.valueOf(((Number) req.get("price")).doubleValue()));
                }
            }
            if (req.containsKey("costPrice") && req.get("costPrice") != null) {
                if (req.get("costPrice") instanceof Number) {
                    variant.setCostPrice(BigDecimal.valueOf(((Number) req.get("costPrice")).doubleValue()));
                }
            }
            
            // Update stock (only if provided)
            if (req.containsKey("stockQuantity") && req.get("stockQuantity") != null) {
                if (req.get("stockQuantity") instanceof Number) {
                    variant.setStockQuantity(((Number) req.get("stockQuantity")).intValue());
                }
            }
            
            // Update flags (only if provided)
            if (req.containsKey("isActive") && req.get("isActive") instanceof Boolean) {
                variant.setActive((Boolean) req.get("isActive"));
            }
            if (req.containsKey("isDefault") && req.get("isDefault") instanceof Boolean) {
                Boolean isDefault = (Boolean) req.get("isDefault");
                variant.setDefault(isDefault);
                
                // If setting this variant as default, unset other default variants for the same product
                if (isDefault && variant.getProduct() != null) {
                    List<ProductVariant> allVariants = productService.getProductVariants(variant.getProduct().getId());
                    for (ProductVariant v : allVariants) {
                        if (!v.getId().equals(variantId) && Boolean.TRUE.equals(v.isDefault())) {
                            v.setDefault(false);
                            productService.saveVariant(v);
                        }
                    }
                }
            }
            
            // Save updated variant
            ProductVariant updated = productService.saveVariant(variant);
            
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Variant updated successfully");
            res.put("variantId", updated.getId());
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    /**
     * Delete variant (admin only)
     */
    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> deleteVariant(@PathVariable Long variantId) {
        try {
            productService.deleteVariant(variantId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}
