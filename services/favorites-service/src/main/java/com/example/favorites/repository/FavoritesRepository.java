package com.example.favorites.repository;

import com.example.favorites.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritesRepository extends JpaRepository<Favorite, Long> {
    
    /**
     * Find favorites by user ID ordered by creation date
     */
    List<Favorite> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find favorites by user ID with pagination
     */
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    /**
     * Check if product is in user's favorites
     */
    boolean existsByUserIdAndProductId(String userId, String productId);
    
    /**
     * Find favorite by user and product
     */
    Optional<Favorite> findByUserIdAndProductId(String userId, String productId);
    
    /**
     * Count favorites by user
     */
    long countByUserId(String userId);
    
    /**
     * Delete all favorites by user
     */
    void deleteByUserId(String userId);
    
    /**
     * Find favorites by product ID
     */
    List<Favorite> findByProductId(String productId);
    
    /**
     * Count favorites by product
     */
    long countByProductId(String productId);
    
    /**
     * Get most favorited products
     */
    @Query("SELECT f.productId as productId, f.productName as productName, f.productPrice as productPrice, f.productImage as productImage, COUNT(f) as favoriteCount " +
           "FROM Favorite f " +
           "GROUP BY f.productId, f.productName, f.productPrice, f.productImage " +
           "ORDER BY COUNT(f) DESC")
    List<Object[]> findMostFavoritedProducts(@Param("limit") int limit);
    
    /**
     * Get recent favorites by user
     */
    @Query("SELECT f FROM Favorite f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findRecentFavoritesByUser(@Param("userId") String userId, Pageable pageable);
    
    /**
     * Get favorites count by product
     */
    @Query("SELECT COUNT(f) FROM Favorite f WHERE f.productId = :productId")
    long getFavoritesCountByProduct(@Param("productId") String productId);
    
    /**
     * Check if user has any favorites
     */
    boolean existsByUserId(String userId);
    
    /**
     * Find favorites created after date
     */
    @Query("SELECT f FROM Favorite f WHERE f.userId = :userId AND f.createdAt > :date ORDER BY f.createdAt DESC")
    List<Favorite> findByUserIdAndCreatedAtAfter(@Param("userId") String userId, @Param("date") java.time.LocalDateTime date);
    
    /**
     * Search favorites by product name
     */
    List<Favorite> findByUserIdAndProductNameContainingIgnoreCase(String userId, String query);
}
