package com.example.brand.controller;

import com.example.brand.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/brands")
public class BrandController {
    
    @Autowired
    private BrandService brandService;
    
    @GetMapping
    public List<Map<String, Object>> getAllBrands() {
        return brandService.getAllBrands().stream().map(brand -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", brand.getId());
            map.put("name", brand.getName());
            map.put("description", brand.getDescription());
            map.put("logoUrl", brand.getLogoUrl());
            map.put("websiteUrl", brand.getWebsiteUrl());
            map.put("isActive", brand.isActive());
            map.put("createdAt", brand.getCreatedAt());
            map.put("updatedAt", brand.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBrandById(@PathVariable Long id) {
        return brandService.findById(id)
                .map(brand -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", brand.getId());
                    map.put("name", brand.getName());
                    map.put("description", brand.getDescription());
                    map.put("logoUrl", brand.getLogoUrl());
                    map.put("websiteUrl", brand.getWebsiteUrl());
                    map.put("isActive", brand.isActive());
                    map.put("createdAt", brand.getCreatedAt());
                    map.put("updatedAt", brand.getUpdatedAt());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public List<Map<String, Object>> searchBrands(@RequestParam String q) {
        return brandService.searchByName(q).stream().map(brand -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", brand.getId());
            map.put("name", brand.getName());
            map.put("description", brand.getDescription());
            map.put("logoUrl", brand.getLogoUrl());
            map.put("websiteUrl", brand.getWebsiteUrl());
            map.put("isActive", brand.isActive());
            map.put("createdAt", brand.getCreatedAt());
            map.put("updatedAt", brand.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Brands API is working!");
    }
}
