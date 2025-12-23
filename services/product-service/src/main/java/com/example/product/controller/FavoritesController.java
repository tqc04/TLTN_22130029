package com.example.product.controller;

import com.example.product.entity.Favorite;
import com.example.product.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {

    @Autowired
    private FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<Favorite>> list(@RequestParam String userId) {
        return ResponseEntity.ok(favoriteService.listByUser(userId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@RequestBody Map<String, Object> body) {
        String userId = body.get("userId").toString();
        String productId = body.get("productId").toString();
        boolean added = favoriteService.toggle(userId, productId);
        return ResponseEntity.ok(Map.of("added", added));
    }
}


