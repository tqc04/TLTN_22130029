package com.example.product.controller;

import com.example.product.entity.Brand;
import com.example.product.entity.Category;
import com.example.product.repository.BrandRepository;
import com.example.product.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Meta endpoints for product catalog.
 * These are served by product-service and backed by product_db tables (brands/categories used by products.brandId/categoryId).
 * We expose them under /api/products/meta/* to avoid gateway route conflicts with brand-service/category-service.
 */
@RestController
@RequestMapping("/api/products/meta")
public class ProductMetaController {

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/brands")
    public ResponseEntity<List<Brand>> brands() {
        return ResponseEntity.ok(brandRepository.findAll());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }
}


