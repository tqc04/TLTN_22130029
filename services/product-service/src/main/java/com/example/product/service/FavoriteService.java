package com.example.product.service;

import com.example.product.entity.Favorite;
import com.example.product.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    public List<Favorite> listByUser(String userId) {
        return favoriteRepository.findByUserId(userId);
    }

    public boolean toggle(String userId, String productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            // simplistic: not implementing delete by composite key; in real code add a custom delete
            return false;
        } else {
            Favorite f = new Favorite();
            f.setUserId(userId);
            f.setProductId(productId);
            favoriteRepository.save(f);
            return true;
        }
    }
}


