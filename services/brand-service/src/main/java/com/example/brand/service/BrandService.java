package com.example.brand.service;

import com.example.brand.entity.Brand;
import com.example.brand.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BrandService {
    
    @Autowired
    private BrandRepository brandRepository;
    
    public List<Brand> getAllBrands() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc();
    }
    
    public Optional<Brand> findById(Long id) {
        return brandRepository.findById(id);
    }
    
    public Optional<Brand> findByName(String name) {
        return brandRepository.findByName(name);
    }
    
    public Brand save(Brand brand) {
        return brandRepository.save(brand);
    }
    
    public void deleteById(Long id) {
        brandRepository.deleteById(id);
    }
    
    public boolean existsByName(String name) {
        return brandRepository.existsByName(name);
    }
    
    public List<Brand> searchByName(String search) {
        return brandRepository.findActiveBrandsByNameContaining(search);
    }
}
