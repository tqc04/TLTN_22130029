package com.example.product.repository;

import com.example.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    List<ProductImage> findByProductIdOrderByDisplayOrderAscIsPrimaryDesc(String productId);
    
    List<ProductImage> findByProductIdAndIsPrimaryTrue(String productId);
    
    Optional<ProductImage> findFirstByProductIdAndIsPrimaryTrue(String productId);
    
    @Modifying
    @Transactional
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.productId = :productId")
    void unsetAllPrimaryByProductId(@Param("productId") String productId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductImage pi WHERE pi.productId = :productId")
    void deleteAllByProductId(@Param("productId") String productId);
    
    long countByProductId(String productId);
}

