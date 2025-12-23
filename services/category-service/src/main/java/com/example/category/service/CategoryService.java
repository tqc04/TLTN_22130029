package com.example.category.service;

import com.example.category.entity.Category;
import com.example.category.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }
    
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
    
    public Optional<Category> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }
    
    public Category save(Category category) {
        return categoryRepository.save(category);
    }
    
    public void deleteById(Long id) {
        categoryRepository.deleteById(id);
    }
    
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }
    
    public boolean existsBySlug(String slug) {
        return categoryRepository.existsBySlug(slug);
    }
    
    public List<Category> searchByName(String search) {
        return categoryRepository.findActiveCategoriesByNameContaining(search);
    }
}
