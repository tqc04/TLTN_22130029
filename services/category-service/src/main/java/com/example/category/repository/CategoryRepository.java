package com.example.category.repository;

import com.example.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
    
    Optional<Category> findBySlug(String slug);
    
    boolean existsByName(String name);
    
    boolean existsBySlug(String slug);
    
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.name LIKE %:search%")
    List<Category> findActiveCategoriesByNameContaining(String search);
}
