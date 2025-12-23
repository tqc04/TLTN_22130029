package com.example.recommendation.service;

import com.example.recommendation.dto.ProductRecommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Recommendation Service - AI-based Product Recommendations
 * 
 * Sử dụng 2 phương pháp chính:
 * 1. Collaborative Filtering (CF) - Dựa trên hành vi người dùng tương tự
 * 2. Content-Based Filtering (CBF) - Dựa trên đặc tính sản phẩm
 */
@Service
public class RecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${services.product.base-url:http://localhost:8083}")
    private String productServiceUrl;
    
    @Value("${ml.service.base-url:http://localhost:8000}")
    private String mlServiceUrl;
    
    @Value("${services.review.base-url:http://localhost:8095}")
    private String reviewServiceUrl;
    
    /**
     * Track user behavior for Collaborative Filtering
     */
    public void trackUserBehavior(String userId, String productId, String behaviorType) {
        try {
            if (redisTemplate != null) {
                // Store user behavior
                String key = "user:behavior:" + userId;
                String behaviorKey = key + ":" + behaviorType;
                
                redisTemplate.opsForSet().add(key, productId);
                redisTemplate.opsForSet().add(behaviorKey, productId);
                redisTemplate.expire(key, 90, TimeUnit.DAYS);
                redisTemplate.expire(behaviorKey, 90, TimeUnit.DAYS);
                
                // Track product popularity for trending
                String productKey = "product:popularity:" + productId;
                redisTemplate.opsForValue().increment(productKey);
                redisTemplate.expire(productKey, 90, TimeUnit.DAYS);
            }
            logger.debug("Tracked behavior: user={}, product={}, type={}", userId, productId, behaviorType);
        } catch (Exception e) {
            logger.error("Error tracking user behavior: {}", e.getMessage());
        }
    }
    
    /**
     * Get personalized recommendations using AI
     * 
     * Tích hợp AI để gợi ý sản phẩm dựa trên hành vi người dùng:
     * 
     * Strategy (AI-based):
     * 1. Collaborative Filtering (ML-based) - Sử dụng SVD model để phân tích hành vi users tương tự
     * 2. Content-Based Filtering - Phân tích đặc tính sản phẩm (category, brand, price, description)
     * 3. Hybrid Approach - Kết hợp CF và CBF để tăng độ chính xác
     */
    public List<ProductRecommendation> getPersonalizedRecommendations(String userId, int limit) {
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        // ============================================
        // METHOD 1: COLLABORATIVE FILTERING (CF) - AI
        // ============================================
        // Sử dụng SVD model (AI) để phân tích patterns từ hành vi users tương tự
        // Tìm users có hành vi tương tự, recommend products họ đã thích
        try {
            List<ProductRecommendation> cfRecommendations = getCollaborativeFilteringRecommendations(userId, limit);
            if (!cfRecommendations.isEmpty()) {
                logger.info("Returning {} Collaborative Filtering (AI) recommendations for user {}", 
                    cfRecommendations.size(), userId);
                return cfRecommendations;
            }
        } catch (Exception e) {
            logger.warn("Collaborative Filtering failed: {}", e.getMessage());
        }
        
        // ============================================
        // METHOD 2: HYBRID APPROACH (CF + CBF)
        // ============================================
        // Kết hợp Collaborative Filtering và Content-Based Filtering
        // Để tăng độ chính xác và coverage
        try {
            List<ProductRecommendation> hybridRecommendations = getHybridRecommendations(userId, limit);
            if (!hybridRecommendations.isEmpty()) {
                logger.info("Returning {} Hybrid (CF+CBF) recommendations for user {}", 
                    hybridRecommendations.size(), userId);
                return hybridRecommendations;
            }
        } catch (Exception e) {
            logger.warn("Hybrid recommendation failed: {}", e.getMessage());
        }
        
        // ============================================
        // METHOD 3: CONTENT-BASED FILTERING (CBF) - AI
        // ============================================
        // Phân tích đặc tính sản phẩm user đã tương tác
        // Recommend products tương tự về category, brand, price, description
        try {
            List<ProductRecommendation> cbfRecommendations = getContentBasedRecommendations(userId, limit);
            if (!cbfRecommendations.isEmpty()) {
                logger.info("Returning {} Content-Based (AI) recommendations for user {}", 
                    cbfRecommendations.size(), userId);
                return cbfRecommendations;
            }
        } catch (Exception e) {
            logger.warn("Content-Based Filtering failed: {}", e.getMessage());
        }
        
        logger.warn("No recommendations available for user {} - no user behavior data", userId);
        return recommendations;
    }
    
    /**
     * COLLABORATIVE FILTERING (CF) - AI-based
     * 
     * Sử dụng AI (SVD model) để phân tích hành vi người dùng và gợi ý sản phẩm:
     * 
     * Logic AI:
     * 1. Gọi Python ML Service với SVD model đã train từ reviews/ratings
     * 2. Model phân tích patterns từ hành vi của users tương tự
     * 3. Predict rating mà user sẽ đánh giá cho mỗi product
     * 4. Rank và recommend top N products có predicted rating cao nhất
     * 
     * Fallback: Redis-based CF (tìm similar users bằng Jaccard similarity)
     */
    private List<ProductRecommendation> getCollaborativeFilteringRecommendations(String userId, int limit) {
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        try {
            // Step 1: Try ML-based Collaborative Filtering (Python service)
            // Model đã được train từ reviews/ratings thực tế
            Map<String, Object> req = new HashMap<>();
            req.put("userId", String.valueOf(userId));
            req.put("limit", limit);
            
            String url = mlServiceUrl + "/recommend";
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(url, req, Map.class);
            
            if (resp != null && resp.containsKey("recommendations")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recs = (List<Map<String, Object>>) resp.get("recommendations");
                
                // Check if recs is not null and not empty
                if (recs != null && !recs.isEmpty()) {
                    for (Map<String, Object> r : recs) {
                        if (r == null) continue;
                        
                        String productIdStr = String.valueOf(r.get("productId"));
                        if (productIdStr == null || productIdStr.equals("null")) continue;
                        
                        // Fix: Use normalizedScore (0-1) if available, otherwise normalize score (1-5) to (0-1)
                        Double score = null;
                        if (r.get("normalizedScore") != null) {
                            score = Double.valueOf(r.get("normalizedScore").toString());
                        } else if (r.get("score") != null) {
                            // Normalize from 1-5 scale to 0-1 scale
                            Double rawScore = Double.valueOf(r.get("score").toString());
                            score = rawScore / 5.0;
                        } else {
                            score = 0.0;
                        }
                        
                        ProductRecommendation pr = enrichProductRecommendation(productIdStr, score);
                        if (pr != null) {
                            pr.setReason("Collaborative Filtering - Based on similar users' preferences");
                            pr.setType("COLLABORATIVE_FILTERING");
                            recommendations.add(pr);
                        }
                    }
                    if (!recommendations.isEmpty()) {
                        // Ensure deterministic order and cap to requested limit even if upstream returns extra
                        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                        return recommendations.stream().limit(limit).collect(Collectors.toList());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("ML-based CF failed, trying Redis-based CF: {}", e.getMessage());
        }
        
        // Step 2: Redis-based Collaborative Filtering
        // Tìm similar users từ Redis behavior data
        if (redisTemplate != null) {
            try {
                // Get current user's interacted products
                String userKey = "user:behavior:" + userId;
                Set<Object> userProducts = redisTemplate.opsForSet().members(userKey);
                
                if (userProducts != null && !userProducts.isEmpty()) {
                    // Find similar users (users who interacted with same products)
                    Set<String> similarUsers = findSimilarUsers(userId, userProducts);
                    
                    // Get products from similar users that current user hasn't seen
                    Set<String> candidateProducts = new HashSet<>();
                    // Convert userProducts to Set<String> for proper comparison
                    Set<String> userProductIds = new HashSet<>();
                    for (Object pid : userProducts) {
                        if (pid != null) {
                            userProductIds.add(pid.toString());
                        }
                    }
                    
                    for (String similarUserId : similarUsers) {
                        String similarUserKey = "user:behavior:" + similarUserId;
                        Set<Object> similarUserProducts = redisTemplate.opsForSet().members(similarUserKey);
                        if (similarUserProducts != null) {
                            for (Object productId : similarUserProducts) {
                                if (productId != null) {
                                    String pid = productId.toString();
                                    // Fix: Proper comparison using String set
                                    if (!userProductIds.contains(pid)) {
                                        candidateProducts.add(pid);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Enrich and rank products
                    for (String productId : candidateProducts.stream().limit(limit * 2).toList()) {
                        ProductRecommendation pr = enrichProductRecommendation(productId, 0.8);
                        if (pr != null) {
                            pr.setReason("Collaborative Filtering - Users with similar behavior liked this");
                            pr.setType("COLLABORATIVE_FILTERING");
                            recommendations.add(pr);
                        }
                    }
                    
                    // Sort by score and return top N
                    recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                    return recommendations.stream().limit(limit).collect(Collectors.toList());
                }
            } catch (Exception e) {
                logger.error("Redis-based CF failed: {}", e.getMessage());
            }
        }
        
        return recommendations;
    }
    
    /**
     * HYBRID RECOMMENDATION (CF + CBF) - AI-based
     * 
     * Kết hợp Collaborative Filtering và Content-Based Filtering để tăng độ chính xác:
     * 
     * Logic:
     * 1. Lấy recommendations từ CF (AI model)
     * 2. Lấy recommendations từ CBF (similarity analysis)
     * 3. Kết hợp và re-rank dựa trên weighted score
     * 4. Return top N hybrid recommendations
     */
    private List<ProductRecommendation> getHybridRecommendations(String userId, int limit) {
        List<ProductRecommendation> hybridRecommendations = new ArrayList<>();
        Map<String, ProductRecommendation> productMap = new HashMap<>();
        
        try {
            // Get CF recommendations (weight: 60%)
            List<ProductRecommendation> cfRecs = getCollaborativeFilteringRecommendations(userId, limit * 2);
            for (ProductRecommendation pr : cfRecs) {
                pr.setScore(pr.getScore() * 0.6); // CF weight: 60%
                productMap.put(pr.getProductId(), pr);
            }
            
            // Get CBF recommendations (weight: 40%)
            List<ProductRecommendation> cbfRecs = getContentBasedRecommendations(userId, limit * 2);
            for (ProductRecommendation pr : cbfRecs) {
                String pid = pr.getProductId();
                if (productMap.containsKey(pid)) {
                    // Combine scores if product appears in both
                    ProductRecommendation existing = productMap.get(pid);
                    existing.setScore(existing.getScore() + pr.getScore() * 0.4);
                    existing.setReason("Hybrid (CF + CBF) - Combined AI recommendation");
                } else {
                    pr.setScore(pr.getScore() * 0.4); // CBF weight: 40%
                    pr.setReason("Hybrid (CBF) - Similar to your preferences");
                    productMap.put(pid, pr);
                }
            }
            
            // Sort by combined score and return top N
            hybridRecommendations = new ArrayList<>(productMap.values());
            hybridRecommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            hybridRecommendations = hybridRecommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            // Set type for all hybrid recommendations
            for (ProductRecommendation pr : hybridRecommendations) {
                pr.setType("HYBRID");
            }
            
        } catch (Exception e) {
            logger.error("Hybrid recommendation failed: {}", e.getMessage());
        }
        
        return hybridRecommendations;
    }
    
    /**
     * CONTENT-BASED FILTERING (CBF) - AI-based
     * 
     * Phân tích đặc tính sản phẩm để gợi ý sản phẩm tương tự:
     * 
     * Logic AI:
     * 1. Lấy products user đã tương tác (xem, mua, review)
     * 2. Phân tích đặc tính: category, brand, price range, description
     * 3. Tính similarity score dựa trên features (AI similarity analysis)
     * 4. Tìm products tương tự về đặc tính
     * 5. Recommend products tương tự nhất
     */
    private List<ProductRecommendation> getContentBasedRecommendations(String userId, int limit) {
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        try {
            // Step 1: Get products user has interacted with
            List<String> interactedProducts = getUserInteractedProducts(userId);
            
            if (interactedProducts.isEmpty()) {
                // User mới, chưa có hành vi -> recommend popular products
                return getPopularProducts(limit);
            }
            
            // Step 2: For each interacted product, find similar products
            Set<String> candidateProducts = new HashSet<>();
            Map<String, Double> productScores = new HashMap<>();
            
            for (String productId : interactedProducts) {
                List<ProductRecommendation> similarProducts = getSimilarProducts(productId, 5);
                for (ProductRecommendation similar : similarProducts) {
                    String similarId = similar.getProductId();
                    if (!interactedProducts.contains(similarId)) {
                        candidateProducts.add(similarId);
                        // Accumulate scores (products similar to multiple user's products get higher score)
                        productScores.put(similarId, 
                            productScores.getOrDefault(similarId, 0.0) + similar.getScore());
                    }
                }
            }
            
            // Step 3: Rank by similarity score
            List<Map.Entry<String, Double>> sorted = productScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
            
            for (Map.Entry<String, Double> entry : sorted) {
                ProductRecommendation pr = enrichProductRecommendation(entry.getKey(), entry.getValue());
                if (pr != null) {
                    pr.setReason("Content-Based Filtering - Similar to products you've interacted with");
                    pr.setType("CONTENT_BASED");
                    recommendations.add(pr);
                }
            }
            
        } catch (Exception e) {
            logger.error("Content-Based Filtering failed: {}", e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Get products user has interacted with (from reviews, orders, behaviors)
     */
    private List<String> getUserInteractedProducts(String userId) {
        Set<String> products = new HashSet<>();
        
        // From reviews
        try {
            String url = reviewServiceUrl + "/api/reviews/user/" + userId;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reviews = restTemplate.getForObject(url, List.class);
            if (reviews != null) {
                for (Map<String, Object> review : reviews) {
                    Object productId = review.get("productId");
                    if (productId != null) {
                        products.add(productId.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get user reviews: {}", e.getMessage());
        }
        
        // From Redis behaviors
        if (redisTemplate != null) {
            try {
                String key = "user:behavior:" + userId;
                Set<Object> behaviorProducts = redisTemplate.opsForSet().members(key);
                if (behaviorProducts != null) {
                    for (Object productId : behaviorProducts) {
                        products.add(productId.toString());
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to get user behaviors from Redis: {}", e.getMessage());
            }
        }
        
        return new ArrayList<>(products);
    }
    
    /**
     * Find similar users based on behavior (users who interacted with similar products)
     */
    private Set<String> findSimilarUsers(String userId, Set<Object> userProducts) {
        Set<String> similarUsers = new HashSet<>();
        
        if (redisTemplate == null || userProducts == null || userProducts.isEmpty()) {
            return similarUsers;
        }
        
        try {
            // Get all user behavior keys
            Set<String> allUserKeys = redisTemplate.keys("user:behavior:*");
            if (allUserKeys == null || allUserKeys.isEmpty()) {
                return similarUsers;
            }
            
            // For each user, calculate similarity (Jaccard similarity)
            for (String otherUserKey : allUserKeys) {
                String otherUserId = otherUserKey.replace("user:behavior:", "");
                if (otherUserId.equals(userId)) {
                    continue;
                }
                
                Set<Object> otherUserProducts = redisTemplate.opsForSet().members(otherUserKey);
                if (otherUserProducts != null && !otherUserProducts.isEmpty()) {
                    // Calculate Jaccard similarity
                    Set<Object> intersection = new HashSet<>(userProducts);
                    intersection.retainAll(otherUserProducts);
                    
                    Set<Object> union = new HashSet<>(userProducts);
                    union.addAll(otherUserProducts);
                    
                    if (!union.isEmpty()) {
                        double similarity = (double) intersection.size() / union.size();
                        // Consider users with similarity > 0.2 as similar
                        if (similarity > 0.2) {
                            similarUsers.add(otherUserId);
                        }
                    }
                }
                
                // Limit to top 10 similar users for performance
                if (similarUsers.size() >= 10) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error finding similar users: {}", e.getMessage());
        }
        
        return similarUsers;
    }
    
    /**
     * Get similar products using Content-Based Filtering (AI similarity analysis)
     * 
     * Phân tích đặc tính sản phẩm và tính similarity score:
     * - Category match: 40% weight
     * - Brand match: 30% weight  
     * - Price similarity (within 30%): 20% weight
     * - Description keywords: 10% weight
     * 
     * Compares: category, brand, price range, description
     */
    public List<ProductRecommendation> getSimilarProducts(String productId, int limit) {
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        try {
            // Get current product details
            @SuppressWarnings("unchecked")
            Map<String, Object> currentProduct = restTemplate.getForObject(
                productServiceUrl + "/api/products/" + productId, Map.class);
            
            if (currentProduct == null) {
                return recommendations;
            }
            
            Long currentCategoryId = currentProduct.get("categoryId") != null ? 
                Long.valueOf(currentProduct.get("categoryId").toString()) : null;
            Long currentBrandId = currentProduct.get("brandId") != null ? 
                Long.valueOf(currentProduct.get("brandId").toString()) : null;
            
            // Fix: Null check for price to avoid NullPointerException
            Object priceObj = currentProduct.get("price");
            if (priceObj == null) {
                logger.warn("Product {} has null price, skipping similarity calculation", productId);
                return recommendations;
            }
            BigDecimal currentPrice = new BigDecimal(priceObj.toString());
            String currentDescription = currentProduct.get("description") != null ? 
                currentProduct.get("description").toString().toLowerCase() : "";
            
            // Get candidate products
            @SuppressWarnings("unchecked")
            Map<String, Object> productResponse = restTemplate.getForObject(
                productServiceUrl + "/api/products?page=0&size=" + (limit * 3), Map.class);
            
            if (productResponse != null && productResponse.containsKey("content")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> products = (List<Map<String, Object>>) productResponse.get("content");
                
                if (products != null) {
                    List<ProductRecommendation> scoredProducts = new ArrayList<>();
                    
                    for (Map<String, Object> product : products) {
                        // Fix: Null check for product ID to avoid NullPointerException
                        Object idObj = product.get("id");
                        if (idObj == null) {
                            continue; // Skip products without ID
                        }
                        String pid = idObj.toString();
                        if (pid.equals(productId)) {
                            continue; // Skip current product
                        }
                        
                        // CONTENT-BASED FILTERING: AI Similarity Scoring
                        // Phân tích đặc tính sản phẩm và tính similarity score
                        double score = 0.0;
                        
                        // Category match (40% weight) - Same category = high similarity
                        Long productCategoryId = product.get("categoryId") != null ? 
                            Long.valueOf(product.get("categoryId").toString()) : null;
                        if (currentCategoryId != null && productCategoryId != null && 
                            currentCategoryId.equals(productCategoryId)) {
                            score += 0.4;
                        }
                        
                        // Brand match (30% weight) - Same brand = high similarity
                        Long productBrandId = product.get("brandId") != null ? 
                            Long.valueOf(product.get("brandId").toString()) : null;
                        if (currentBrandId != null && productBrandId != null && 
                            currentBrandId.equals(productBrandId)) {
                            score += 0.3;
                        }
                        
                        // Price similarity (20% weight) - Within 30% price range
                        // Products with similar price are more likely to be preferred
                        Object productPriceObj = product.get("price");
                        if (productPriceObj != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                            try {
                                BigDecimal productPrice = new BigDecimal(productPriceObj.toString());
                                double priceDiff = Math.abs(productPrice.subtract(currentPrice).doubleValue()) / 
                                    currentPrice.doubleValue();
                                if (priceDiff <= 0.3) {
                                    // Linear decay: closer price = higher score
                                    score += 0.2 * (1 - priceDiff / 0.3);
                                }
                            } catch (NumberFormatException e) {
                                logger.debug("Invalid price format for product {}: {}", pid, e.getMessage());
                            }
                        }
                        
                        // Description similarity (10% weight) - Keyword matching
                        // Analyze text similarity using keyword overlap
                        String productDescription = product.get("description") != null ? 
                            product.get("description").toString().toLowerCase() : "";
                        if (!currentDescription.isEmpty() && !productDescription.isEmpty()) {
                            String[] currentWords = currentDescription.split("\\s+");
                            int matchingWords = 0;
                            for (String word : currentWords) {
                                // Only match meaningful words (length > 3)
                                if (word.length() > 3 && productDescription.contains(word)) {
                                    matchingWords++;
                                }
                            }
                            if (currentWords.length > 0) {
                                // Score based on keyword overlap ratio
                                score += 0.1 * (matchingWords / (double) currentWords.length);
                            }
                        }
                        
                        // Only add products with meaningful similarity
                        if (score > 0.2) {
                            ProductRecommendation pr = enrichProductRecommendation(pid, score);
                            if (pr != null) {
                                pr.setReason("Similar product (Content-Based)");
                                pr.setType("CONTENT_BASED");
                                scoredProducts.add(pr);
                            }
                        }
                    }
                    
                    // Sort by score and return top N
                    scoredProducts.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
                    return scoredProducts.stream().limit(limit).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            logger.error("Error getting similar products: {}", e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Get popular products (fallback for new users)
     */
    private List<ProductRecommendation> getPopularProducts(int limit) {
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        try {
            // Get products sorted by popularity (from Redis or reviews)
            @SuppressWarnings("unchecked")
            Map<String, Object> productResponse = restTemplate.getForObject(
                productServiceUrl + "/api/products?page=0&size=" + limit, Map.class);
            
            if (productResponse != null && productResponse.containsKey("content")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> products = (List<Map<String, Object>>) productResponse.get("content");
                
                if (products != null) {
                    // Sort by average rating if available
                    products.sort((p1, p2) -> {
                        try {
                            // Fix: Null checks for product IDs to avoid NullPointerException
                            Object id1Obj = p1.get("id");
                            Object id2Obj = p2.get("id");
                            if (id1Obj == null || id2Obj == null) {
                                return 0;
                            }
                            Double r1 = getProductAverageRating(id1Obj.toString());
                            Double r2 = getProductAverageRating(id2Obj.toString());
                            if (r1 != null && r2 != null) {
                                return Double.compare(r2, r1);
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
                        return 0;
                    });
                    
                    for (Map<String, Object> product : products.stream().limit(limit).toList()) {
                        // Fix: Null check for product ID to avoid NullPointerException
                        Object idObj = product.get("id");
                        if (idObj == null) {
                            continue; // Skip products without ID
                        }
                        String productId = idObj.toString();
                        Double avgRating = getProductAverageRating(productId);
                        double score = avgRating != null ? avgRating / 5.0 : 0.7;
                        
                        ProductRecommendation pr = enrichProductRecommendation(productId, score);
                        if (pr != null) {
                            pr.setReason("Popular products");
                            pr.setType("POPULAR");
                            recommendations.add(pr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting popular products: {}", e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Enrich product recommendation with details from Product Service
     */
    private ProductRecommendation enrichProductRecommendation(String productId, double score) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> product = restTemplate.getForObject(
                productServiceUrl + "/api/products/" + productId, Map.class);
            
            if (product != null) {
                // Fix: Null checks for name and price to avoid NullPointerException
                Object nameObj = product.get("name");
                Object priceObj = product.get("price");
                if (nameObj == null || priceObj == null) {
                    logger.debug("Product {} missing name or price, skipping enrichment", productId);
                    return null;
                }
                
                ProductRecommendation pr = new ProductRecommendation();
                pr.setProductId(productId);
                pr.setProductName(nameObj.toString());
                try {
                    pr.setPrice(new BigDecimal(priceObj.toString()));
                } catch (NumberFormatException e) {
                    logger.debug("Invalid price format for product {}: {}", productId, e.getMessage());
                    return null;
                }
                pr.setProductImage(product.get("imageUrl") != null ? product.get("imageUrl").toString() : "");
                pr.setScore(Math.min(1.0, Math.max(0.0, score)));
                return pr;
            }
        } catch (Exception e) {
            logger.debug("Failed to enrich product {}: {}", productId, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get average rating for a product
     */
    private Double getProductAverageRating(String productId) {
        try {
            String url = reviewServiceUrl + "/api/reviews/product/" + productId + "/stats";
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = restTemplate.getForObject(url, Map.class);
            
            if (stats != null && stats.containsKey("averageRating")) {
                Object avgRating = stats.get("averageRating");
                if (avgRating != null) {
                    return Double.valueOf(avgRating.toString());
                }
            }
        } catch (Exception e) {
            // Fallback: try product service
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> product = restTemplate.getForObject(
                    productServiceUrl + "/api/products/" + productId, Map.class);
                if (product != null && product.containsKey("averageRating")) {
                    Object avgRating = product.get("averageRating");
                    if (avgRating != null) {
                        return Double.valueOf(avgRating.toString());
                    }
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        return null;
    }
}
