package com.example.product.repository;

import com.example.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.isActive = true")
    List<ProductVariant> findByProductIdAndIsActiveTrue(@Param("productId") String productId);
    
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId")
    List<ProductVariant> findByProductId(@Param("productId") String productId);
    
    Optional<ProductVariant> findBySku(String sku);
    
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.isDefault = true")
    Optional<ProductVariant> findDefaultVariantByProductId(@Param("productId") String productId);
    
    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.color = :color AND v.size = :size")
    Optional<ProductVariant> findByProductIdAndColorAndSize(@Param("productId") String productId, 
                                                           @Param("color") String color, 
                                                           @Param("size") String size);
    
    @Query("SELECT SUM(v.stockQuantity) FROM ProductVariant v WHERE v.product.id = :productId AND v.isActive = true")
    Integer getTotalStockByProductId(@Param("productId") String productId);
}
