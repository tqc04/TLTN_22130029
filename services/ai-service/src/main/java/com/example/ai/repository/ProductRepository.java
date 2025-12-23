package com.example.ai.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional
public class ProductRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Get products by search keyword
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchProducts(String keyword, int limit) {
        // MySQL doesn't allow parameter binding for LIMIT - use string substitution
        // Use LOWER() for case-insensitive search to ensure we find products like "iPhone", "MacBook", etc.
        String softDeleteFilter = hasIsDeletedColumn()
            ? "              AND (p.is_deleted = 0 OR p.is_deleted IS NULL)\n"
            : "";
        
        String sql = String.format("""
            SELECT p.id, p.name, p.description, p.price, p.sale_price, 
                   p.category_id, p.brand_id, p.stock_quantity, p.average_rating,
                   p.view_count, p.purchase_count, p.sku, p.image_url, p.review_count,
                   c.name as category_name, b.name as brand_name
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN brands b ON p.brand_id = b.id
            WHERE p.is_active = 1
%s
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%%', :keyword, '%%')) 
               OR LOWER(p.description) LIKE LOWER(CONCAT('%%', :keyword, '%%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%%', :keyword, '%%')))
            ORDER BY 
              CASE 
                WHEN LOWER(p.name) LIKE LOWER(CONCAT(:keyword, '%%')) THEN 1
                WHEN LOWER(p.name) LIKE LOWER(CONCAT('%%', :keyword, '%%')) THEN 2
                WHEN LOWER(p.sku) LIKE LOWER(CONCAT(:keyword, '%%')) THEN 3
                ELSE 4
              END,
              p.created_at DESC
            LIMIT %d
            """, softDeleteFilter, Math.max(1, Math.min(limit, 100))); // Limit between 1-100 for safety
        
        try {
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("keyword", keyword);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            List<Map<String, Object>> products = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", row[0]);
                product.put("name", row[1]);
                product.put("description", row[2]);
                product.put("price", row[3]);
                product.put("sale_price", row[4]);
                product.put("category_id", row[5]);
                product.put("brand_id", row[6]);
                product.put("stock_quantity", row[7]);
                product.put("average_rating", row[8]);
                product.put("view_count", row[9]);
                product.put("purchase_count", row[10]);
                product.put("sku", row[11] != null ? row[11] : "");
                product.put("image_url", row[12] != null ? row[12] : "");
                product.put("review_count", row[13] != null ? row[13] : 0);
                product.put("category_name", row[14] != null ? row[14] : "");
                product.put("brand_name", row[15] != null ? row[15] : "");
                products.add(product);
            }
            
            return products;
        } catch (Exception e) {
            // Return empty list if query fails
            return new ArrayList<>();
        }
    }
    
    private volatile Boolean isDeletedColumnPresent;
    
    private boolean hasIsDeletedColumn() {
        if (isDeletedColumnPresent != null) {
            return isDeletedColumnPresent;
        }
        
        try {
            Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'products'
                  AND column_name = 'is_deleted'
                """).getSingleResult();
            
            isDeletedColumnPresent = ((Number) result).intValue() > 0;
        } catch (Exception e) {
            isDeletedColumnPresent = false;
        }
        
        return isDeletedColumnPresent;
    }
    
    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductById(Long productId) {
        String sql = """
            SELECT p.id, p.name, p.description, p.price, p.sale_price, p.stock_quantity,
                   p.category_id, p.brand_id, p.average_rating
            FROM products p
            WHERE p.id = :productId
            LIMIT 1
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            Map<String, Object> product = new HashMap<>();
            product.put("id", row[0]);
            product.put("name", row[1]);
            product.put("description", row[2]);
            product.put("price", row[3]);
            product.put("sale_price", row[4]);
            product.put("stock_quantity", row[5]);
            product.put("category_id", row[6]);
            product.put("brand_id", row[7]);
            product.put("average_rating", row[8]);
            return product;
        }
        
        return null;
    }
    
    /**
     * Get popular products
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPopularProducts(int limit) {
        String sql = String.format("""
            SELECT p.id, p.name, p.price, p.sale_price, p.average_rating,
                   p.view_count, p.purchase_count, p.description, p.stock_quantity,
                   p.sku, p.image_url, p.review_count,
                   c.name as category_name, b.name as brand_name
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            LEFT JOIN brands b ON p.brand_id = b.id
            WHERE p.is_active = 1
            ORDER BY p.purchase_count DESC, p.view_count DESC
            LIMIT %d
            """, Math.max(1, Math.min(limit, 100)));
        
        Query query = entityManager.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> products = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", row[0]);
            product.put("name", row[1]);
            product.put("price", row[2]);
            product.put("sale_price", row[3]);
            product.put("average_rating", row[4]);
            product.put("view_count", row[5]);
            product.put("purchase_count", row[6]);
            product.put("description", row[7] != null ? row[7] : "");
            product.put("stock_quantity", row[8] != null ? row[8] : 0);
            product.put("sku", row[9] != null ? row[9] : "");
            product.put("image_url", row[10] != null ? row[10] : "");
            product.put("review_count", row[11] != null ? row[11] : 0);
            product.put("category_name", row[12] != null ? row[12] : "");
            product.put("brand_name", row[13] != null ? row[13] : "");
            products.add(product);
        }
        
        return products;
    }
    
    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsByCategory(Long categoryId, int limit) {
        String sql = String.format("""
            SELECT p.id, p.name, p.price, p.sale_price, p.stock_quantity,
                   p.average_rating, p.description
            FROM products p
            WHERE p.category_id = :categoryId
              AND p.is_active = 1
            ORDER BY p.created_at DESC
            LIMIT %d
            """, Math.max(1, Math.min(limit, 100)));
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categoryId", categoryId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> products = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", row[0]);
            product.put("name", row[1]);
            product.put("price", row[2]);
            product.put("sale_price", row[3]);
            product.put("stock_quantity", row[4]);
            product.put("average_rating", row[5]);
            product.put("description", row[6]);
            products.add(product);
        }
        
        return products;
    }
    
    /**
     * Get products by brand
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsByBrand(Long brandId, int limit) {
        String sql = String.format("""
            SELECT p.id, p.name, p.price, p.sale_price, p.average_rating,
                   p.view_count, p.purchase_count
            FROM products p
            WHERE p.brand_id = :brandId
              AND p.is_active = 1
            ORDER BY p.created_at DESC
            LIMIT %d
            """, Math.max(1, Math.min(limit, 100)));
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("brandId", brandId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> products = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", row[0]);
            product.put("name", row[1]);
            product.put("price", row[2]);
            product.put("sale_price", row[3]);
            product.put("average_rating", row[4]);
            product.put("view_count", row[5]);
            product.put("purchase_count", row[6]);
            products.add(product);
        }
        
        return products;
    }
    
    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllCategories() {
        String sql = """
            SELECT id, name, description
            FROM categories
            WHERE is_active = 1
            ORDER BY name ASC
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> category = new HashMap<>();
            category.put("id", row[0]);
            category.put("name", row[1]);
            category.put("description", row[2]);
            categories.add(category);
        }
        
        return categories;
    }
    
    /**
     * Get all brands
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllBrands() {
        String sql = """
            SELECT id, name, description
            FROM brands
            WHERE is_active = 1
            ORDER BY name ASC
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> brands = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> brand = new HashMap<>();
            brand.put("id", row[0]);
            brand.put("name", row[1]);
            brand.put("description", row[2]);
            brands.add(brand);
        }
        
        return brands;
    }
    
    /**
     * Get products on sale
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductsOnSale(int limit) {
        String sql = String.format("""
            SELECT p.id, p.name, p.price, p.sale_price, 
                   ROUND(((p.price - p.sale_price) / p.price) * 100) as discount_percent
            FROM products p
            WHERE p.is_active = 1 
              AND p.is_on_sale = 1
            ORDER BY discount_percent DESC
            LIMIT %d
            """, Math.max(1, Math.min(limit, 100)));
        
        Query query = entityManager.createNativeQuery(sql);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> products = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> product = new HashMap<>();
            product.put("id", row[0]);
            product.put("name", row[1]);
            product.put("price", row[2]);
            product.put("sale_price", row[3]);
            product.put("discount_percent", row[4]);
            products.add(product);
        }
        
        return products;
    }
}

