package com.example.product.repository;

import com.example.product.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(String userId);
    boolean existsByUserIdAndProductId(String userId, String productId);
}


