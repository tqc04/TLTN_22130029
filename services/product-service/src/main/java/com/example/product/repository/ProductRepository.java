package com.example.product.repository;

import com.example.product.entity.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Page<Product> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:search, '%')) THEN 3 " +
           "ELSE 4 END, p.name ASC")
    Page<Product> findActiveProductsBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND p.categoryId IN (SELECT c.id FROM Category c WHERE c.name = :category)")
    Page<Product> findActiveProductsByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "p.categoryId IN (SELECT c.id FROM Category c WHERE c.name = :category) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:search, '%')) THEN 3 " +
           "ELSE 4 END, p.name ASC")
    Page<Product> findActiveProductsBySearchAndCategory(@Param("search") String search, @Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND p.isFeatured = true ORDER BY p.createdAt DESC")
    Page<Product> findFeatured(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND p.isOnSale = true AND (p.saleStartAt IS NULL OR p.saleStartAt <= CURRENT_TIMESTAMP) AND (p.saleEndAt IS NULL OR p.saleEndAt > CURRENT_TIMESTAMP) ORDER BY p.createdAt DESC")
    Page<Product> findOnSale(Pageable pageable);

    /**
     * Find products on sale within a specific time window (flash sale slot).
     * This does NOT use CURRENT_TIMESTAMP; the window is provided by caller.
     */
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND p.isOnSale = true " +
           "AND p.saleStartAt IS NOT NULL AND p.saleEndAt IS NOT NULL " +
           "AND p.saleStartAt < :endAt AND p.saleEndAt > :startAt " +
           "ORDER BY p.createdAt DESC")
    Page<Product> findOnSaleInWindow(@Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt, Pageable pageable);
    
    /**
     * Find products with expired sales (for scheduled task)
     */
    @Query("SELECT p FROM Product p WHERE p.isOnSale = true " +
           "AND p.saleEndAt IS NOT NULL " +
           "AND p.saleEndAt <= CURRENT_TIMESTAMP")
    List<Product> findExpiredSaleProducts();

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = :stockQuantity WHERE p.isActive = true")
    int updateAllStock(@Param("stockQuantity") Integer stockQuantity);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND p.categoryId = :categoryId AND p.id != :excludeProductId ORDER BY p.createdAt DESC")
    Page<Product> findRelatedProductsByCategory(@Param("categoryId") Long categoryId, @Param("excludeProductId") String excludeProductId, Pageable pageable);

    // Search with brand filter
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "p.brandId IN (SELECT b.id FROM Brand b WHERE b.name = :brand) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:search, '%')) THEN 3 " +
           "ELSE 4 END, p.name ASC")
    Page<Product> findActiveProductsBySearchAndBrand(@Param("search") String search, @Param("brand") String brand, Pageable pageable);

    // Search with category and brand
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "p.categoryId IN (SELECT c.id FROM Category c WHERE c.name = :category) AND " +
           "p.brandId IN (SELECT b.id FROM Brand b WHERE b.name = :brand) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:search, '%')) THEN 3 " +
           "ELSE 4 END, p.name ASC")
    Page<Product> findActiveProductsBySearchCategoryAndBrand(@Param("search") String search, @Param("category") String category, @Param("brand") String brand, Pageable pageable);

    // Search with price range
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "(COALESCE(p.salePrice, p.price) >= :minPrice AND COALESCE(p.salePrice, p.price) <= :maxPrice) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:search, '%')) THEN 3 " +
           "ELSE 4 END, p.name ASC")
    Page<Product> findActiveProductsBySearchAndPriceRange(@Param("search") String search, @Param("minPrice") java.math.BigDecimal minPrice, @Param("maxPrice") java.math.BigDecimal maxPrice, Pageable pageable);

    // Full filter: search + category + brand + price range
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "p.categoryId IN (SELECT c.id FROM Category c WHERE c.name = :category) AND " +
           "p.brandId IN (SELECT b.id FROM Brand b WHERE b.name = :brand) AND " +
           "(COALESCE(p.salePrice, p.price) >= :minPrice AND COALESCE(p.salePrice, p.price) <= :maxPrice) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT(:search, '%')) THEN 1 " +
           "WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) THEN 2 " +
           "WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:search, '%')) THEN 3 " +
           "ELSE 4 END, p.name ASC")
    Page<Product> findActiveProductsWithAllFilters(@Param("search") String search, @Param("category") String category, @Param("brand") String brand, @Param("minPrice") java.math.BigDecimal minPrice, @Param("maxPrice") java.math.BigDecimal maxPrice, Pageable pageable);

    // Brand filter only
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "p.brandId IN (SELECT b.id FROM Brand b WHERE b.name = :brand)")
    Page<Product> findActiveProductsByBrand(@Param("brand") String brand, Pageable pageable);

    // Category and brand
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND " +
           "p.categoryId IN (SELECT c.id FROM Category c WHERE c.name = :category) AND " +
           "p.brandId IN (SELECT b.id FROM Brand b WHERE b.name = :brand)")
    Page<Product> findActiveProductsByCategoryAndBrand(@Param("category") String category, @Param("brand") String brand, Pageable pageable);
}


