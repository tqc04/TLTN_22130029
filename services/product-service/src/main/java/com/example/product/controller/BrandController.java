package com.example.product.controller;

import com.example.product.entity.Brand;
import com.example.product.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    @Autowired
    private BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<List<Brand>> list() {
        return ResponseEntity.ok(brandRepository.findAll());
    }
}


