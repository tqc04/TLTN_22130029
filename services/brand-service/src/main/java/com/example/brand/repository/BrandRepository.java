package com.example.brand.repository;

import com.example.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    
    List<Brand> findByIsActiveTrueOrderByNameAsc();
    
    Optional<Brand> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT b FROM Brand b WHERE b.isActive = true AND b.name LIKE %:search%")
    List<Brand> findActiveBrandsByNameContaining(String search);
}
