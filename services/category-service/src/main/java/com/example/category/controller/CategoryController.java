package com.example.category.controller;

import com.example.category.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    @GetMapping
    public List<Map<String, Object>> getAllCategories() {
        System.out.println("=== CATEGORIES CONTROLLER HIT ===");
        return categoryService.getAllCategories().stream().map(category -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", category.getId());
            map.put("name", category.getName());
            map.put("description", category.getDescription());
            map.put("slug", category.getSlug());
            map.put("imageUrl", category.getImageUrl());
            map.put("isActive", category.isActive());
            map.put("sortOrder", category.getSortOrder());
            map.put("createdAt", category.getCreatedAt());
            map.put("updatedAt", category.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(category -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", category.getId());
                    map.put("name", category.getName());
                    map.put("description", category.getDescription());
                    map.put("slug", category.getSlug());
                    map.put("imageUrl", category.getImageUrl());
                    map.put("isActive", category.isActive());
                    map.put("sortOrder", category.getSortOrder());
                    map.put("createdAt", category.getCreatedAt());
                    map.put("updatedAt", category.getUpdatedAt());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Map<String, Object>> getCategoryBySlug(@PathVariable String slug) {
        return categoryService.findBySlug(slug)
                .map(category -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", category.getId());
                    map.put("name", category.getName());
                    map.put("description", category.getDescription());
                    map.put("slug", category.getSlug());
                    map.put("imageUrl", category.getImageUrl());
                    map.put("isActive", category.isActive());
                    map.put("sortOrder", category.getSortOrder());
                    map.put("createdAt", category.getCreatedAt());
                    map.put("updatedAt", category.getUpdatedAt());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public List<Map<String, Object>> searchCategories(@RequestParam String q) {
        return categoryService.searchByName(q).stream().map(category -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", category.getId());
            map.put("name", category.getName());
            map.put("description", category.getDescription());
            map.put("slug", category.getSlug());
            map.put("imageUrl", category.getImageUrl());
            map.put("isActive", category.isActive());
            map.put("sortOrder", category.getSortOrder());
            map.put("createdAt", category.getCreatedAt());
            map.put("updatedAt", category.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Categories API is working!");
    }
}
