package com.example.ai.service;

import com.example.ai.entity.AIProvider;
import com.example.ai.entity.AIRequestType;
import com.example.ai.entity.ChatLog;
import com.example.ai.repository.ChatLogRepository;
import com.example.ai.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

@Service
public class ChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ChatLogRepository chatLogRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // In-memory storage for chat sessions (in production, use Redis or database)
    private final Map<String, Map<String, Object>> chatSessions = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> chatHistory = new HashMap<>();
    
    /**
     * Process chat message with intelligent routing: Database-first for products, AI for general questions
     * PUBLIC API (No authentication required)
     */
    @Transactional
    public Map<String, Object> processChatMessage(String userId, String message, String sessionId) {
        ChatLog chatLog = null;
        try {
            // Use anonymous user if userId is null or empty
            final String finalUserId;
            if (userId == null || userId.isEmpty() || userId.equals("0")) {
                finalUserId = "anonymous";
            } else {
                finalUserId = userId;
            }
            
            // Create or get session
            final String finalSessionId = (sessionId == null) ? UUID.randomUUID().toString() : sessionId;
            
            Map<String, Object> session = chatSessions.computeIfAbsent(finalSessionId, k -> {
                Map<String, Object> newSession = new HashMap<>();
                newSession.put("sessionId", finalSessionId);
                newSession.put("userId", finalUserId);
                newSession.put("createdAt", new Date());
                newSession.put("messages", new ArrayList<>());
                return newSession;
            });
            
            // Create chat log entry
            chatLog = new ChatLog(finalUserId, finalSessionId, message);
            
            // Step 1: Check if question is product-related
            boolean isProductRelated = isProductRelatedQuestion(message);
            chatLog.setIsProductRelated(isProductRelated);
            
            String responseText;
            String responseSource;
            boolean usedAI = false;
            boolean foundProducts = false;
            List<String> productIds = new ArrayList<>();
            List<String> productNames = new ArrayList<>();
            
            if (isProductRelated) {
                // Step 2: Search products in database FIRST
                String[] keywords = extractKeywords(message);
                List<Map<String, Object>> foundProductsList = new ArrayList<>();
                boolean isDetailQuery = message.toLowerCase().contains("chi tiết") || 
                                       message.toLowerCase().contains("thông tin") ||
                                       message.toLowerCase().contains("mô tả");
                
                try {
                    logger.info("Searching products for message: '{}' with keywords: {}", message, Arrays.toString(keywords));
                    
                    // Strategy 0: If it's a detail query, try to find exact match first
                    if (isDetailQuery && keywords.length > 0) {
                        // Try to find exact product name match
                        String productNameQuery = String.join(" ", keywords);
                        try {
                            logger.debug("Detail query - searching for exact match: '{}'", productNameQuery);
                            List<Map<String, Object>> exactResults = productRepository.searchProducts(productNameQuery, 20);
                            if (exactResults != null && !exactResults.isEmpty()) {
                                // Filter for exact name matches (case-insensitive)
                                List<Map<String, Object>> exactMatches = new ArrayList<>();
                                for (Map<String, Object> product : exactResults) {
                                    String productName = String.valueOf(product.getOrDefault("name", "")).toLowerCase();
                                    // Check if product name contains all keywords or matches closely
                                    boolean matches = true;
                                    for (String keyword : keywords) {
                                        if (keyword.length() >= 2 && !productName.contains(keyword.toLowerCase())) {
                                            matches = false;
                                            break;
                                        }
                                    }
                                    if (matches) {
                                        exactMatches.add(product);
                                    }
                                }
                                
                                if (!exactMatches.isEmpty()) {
                                    // If we found exact matches, prioritize them
                                    // If only 1 exact match, use only that one
                                    if (exactMatches.size() == 1) {
                                        foundProductsList = exactMatches;
                                        logger.info("Found exact match for detail query: {}", exactMatches.get(0).get("name"));
                                    } else {
                                        // Multiple exact matches - use them but limit to top 3
                                        foundProductsList = exactMatches.subList(0, Math.min(3, exactMatches.size()));
                                        logger.info("Found {} exact matches for detail query", foundProductsList.size());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Exact match search failed: {}", e.getMessage());
                        }
                    }
                    
                    // Strategy 1: Search with each keyword individually (more flexible)
                    if (foundProductsList.isEmpty() && keywords.length > 0) {
                        for (String keyword : keywords) {
                            try {
                                logger.debug("Searching with keyword: '{}'", keyword);
                                List<Map<String, Object>> results = productRepository.searchProducts(keyword, 10);
                                if (results != null && !results.isEmpty()) {
                                    logger.info("Found {} products with keyword '{}'", results.size(), keyword);
                                    foundProductsList.addAll(results);
                                } else {
                                    logger.debug("No products found with keyword '{}'", keyword);
                                }
                            } catch (Exception e) {
                                logger.warn("Search failed for keyword '{}': {}", keyword, e.getMessage());
                            }
                        }
                    }
                    
                    // Strategy 2: If no results, try searching with full message (fallback)
                    if (foundProductsList.isEmpty()) {
                        try {
                            logger.debug("No results with keywords, trying full message: '{}'", message);
                            List<Map<String, Object>> results = productRepository.searchProducts(message, 10);
                            if (results != null && !results.isEmpty()) {
                                logger.info("Found {} products with full message", results.size());
                                foundProductsList.addAll(results);
                            }
                        } catch (Exception e) {
                            logger.warn("Search failed for full message: {}", e.getMessage());
                        }
                    }
                    
                    // Strategy 3: If still no results, try searching with individual words from message
                    if (foundProductsList.isEmpty() && message.length() > 0) {
                        logger.debug("Trying individual words from message");
                        String[] words = message.toLowerCase().split("\\s+");
                        for (String word : words) {
                            if (word.length() >= 3) { // Only search words with 3+ characters
                                try {
                                    List<Map<String, Object>> results = productRepository.searchProducts(word, 10);
                                    if (results != null && !results.isEmpty()) {
                                        logger.info("Found {} products with word '{}'", results.size(), word);
                                        foundProductsList.addAll(results);
                                        break; // Stop after first successful search
                                    }
                                } catch (Exception e) {
                                    // Ignore individual word search errors
                                }
                            }
                        }
                    }
                    
                    logger.info("Total products found after all search strategies: {}", foundProductsList.size());
                    
                    // Remove duplicates based on product ID
                    if (!foundProductsList.isEmpty()) {
                        Map<Object, Map<String, Object>> uniqueProducts = new HashMap<>();
                        for (Map<String, Object> product : foundProductsList) {
                            Object id = product.get("id");
                            if (id != null && !uniqueProducts.containsKey(id)) {
                                uniqueProducts.put(id, product);
                            }
                        }
                        foundProductsList = new ArrayList<>(uniqueProducts.values());
                    }
                } catch (Exception ex) {
                    logger.error("Product search failed: {}", ex.getMessage(), ex);
                }
                
                if (foundProductsList != null && !foundProductsList.isEmpty()) {
                    // Step 3: Format response from database data (NO AI)
                    foundProducts = true;
                    responseText = formatProductResponseFromDatabase(foundProductsList, message);
                    responseSource = "DATABASE";
                    usedAI = false;
                    
                    // Collect product IDs and names for logging
                    for (Map<String, Object> product : foundProductsList) {
                        Object id = product.get("id");
                        Object name = product.get("name");
                        if (id != null) productIds.add(String.valueOf(id));
                        if (name != null) productNames.add(String.valueOf(name));
                    }
                } else {
                    // Step 4: No products found - check if it's a generic detail query
                    boolean isGenericDetailQuery = message.toLowerCase().matches(".*(chi tiết|thông tin|mô tả).*sản phẩm.*") &&
                                                  !message.toLowerCase().matches(".*(laptop|điện thoại|phone|iphone|samsung|xiaomi|oppo|huawei|dell|lenovo|asus|acer|hp|macbook|ipad|tablet|tai nghe|headphone|đồng hồ|watch|loa|speaker|chuột|mouse|bàn phím|keyboard|màn hình|monitor).*");
                    
                    if (isGenericDetailQuery) {
                        // User asked for product details without specifying which product
                        // Try to suggest popular products
                        try {
                            List<Map<String, Object>> popularProducts = productRepository.getPopularProducts(5);
                            if (popularProducts != null && !popularProducts.isEmpty()) {
                                StringBuilder suggestResponse = new StringBuilder();
                                suggestResponse.append("Bạn muốn xem chi tiết sản phẩm nào? ");
                                suggestResponse.append("Dưới đây là một số sản phẩm phổ biến:\n\n");
                                
                                int count = 1;
                                for (Map<String, Object> product : popularProducts) {
                                    String name = String.valueOf(product.getOrDefault("name", "Sản phẩm"));
                                    Object price = product.get("price");
                                    Object salePrice = product.getOrDefault("sale_price", price);
                                    suggestResponse.append(count++).append(". ").append(name);
                                    if (salePrice != null) {
                                        suggestResponse.append(" - ").append(formatPrice(salePrice)).append(" VND");
                                    }
                                    suggestResponse.append("\n");
                                }
                                
                                suggestResponse.append("\nBạn có thể hỏi chi tiết về bất kỳ sản phẩm nào ở trên!");
                                responseText = suggestResponse.toString();
                            } else {
                                responseText = "Bạn muốn xem chi tiết sản phẩm nào? " +
                                    "Vui lòng cho tôi biết tên sản phẩm cụ thể (ví dụ: iPhone 15, MacBook Pro, Laptop Dell...) " +
                                    "hoặc loại sản phẩm bạn quan tâm (laptop, điện thoại, tai nghe...).";
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to get popular products for suggestion: {}", e.getMessage());
                            responseText = "Bạn muốn xem chi tiết sản phẩm nào? " +
                                "Vui lòng cho tôi biết tên sản phẩm cụ thể (ví dụ: iPhone 15, MacBook Pro, Laptop Dell...) " +
                                "hoặc loại sản phẩm bạn quan tâm (laptop, điện thoại, tai nghe...).";
                        }
                        responseSource = "ASK_FOR_PRODUCT_NAME";
                    } else {
                        // Specific product not found
                        responseText = "Xin lỗi, hiện tại cửa hàng chúng tôi không có sản phẩm này. " +
                            "Bạn có thể thử tìm kiếm với từ khóa khác hoặc liên hệ bộ phận hỗ trợ để được tư vấn thêm.";
                        responseSource = "NO_PRODUCTS";
                    }
                    usedAI = false;
                    foundProducts = false;
                }
            } else {
                // Step 5: General question - use AI
                Map<String, Object> aiResult = aiService.processAIRequest(
                    finalUserId,
                    AIProvider.GEMINI,
                    AIRequestType.CHAT,
                    message,
                    "Bạn là trợ lý AI chuyên nghiệp của cửa hàng điện tử. Trả lời câu hỏi một cách thân thiện và hữu ích bằng tiếng Việt.",
                    3000
                );
                
                boolean aiSuccess = Boolean.TRUE.equals(aiResult.get("success"));
                Object aiResponseObj = aiResult.get("response") != null ? aiResult.get("response") : aiResult.get("text");
                responseText = aiResponseObj != null ? String.valueOf(aiResponseObj) : "";
                
                if (aiSuccess && StringUtils.hasText(responseText)) {
                    responseSource = "AI";
                    usedAI = true;
                } else {
                    // Fallback response if AI fails - use simple rule-based answer first
                    String ruleBased = generateRuleBasedResponse(message);
                    if (StringUtils.hasText(ruleBased)) {
                        responseText = ruleBased;
                        responseSource = "RULE_BASED";
                    } else {
                        responseText = "Xin lỗi, hiện tại tôi chưa thể trả lời câu hỏi này. " +
                            "Bạn có thể mô tả rõ hơn nhu cầu hoặc liên hệ bộ phận hỗ trợ để được trợ giúp nhanh.";
                        responseSource = "FALLBACK";
                    }
                    usedAI = false;
                }
            }
            
            // Update chat log
            chatLog.setAiResponse(responseText);
            chatLog.setUsedAI(usedAI);
            chatLog.setFoundProducts(foundProducts);
            chatLog.setResponseSource(responseSource);
            if (!productIds.isEmpty()) {
                try {
                    chatLog.setProductIds(objectMapper.writeValueAsString(productIds));
                    chatLog.setProductNames(objectMapper.writeValueAsString(productNames));
                } catch (Exception e) {
                    logger.warn("Failed to serialize product IDs: {}", e.getMessage());
                }
            }
            
            // Save chat log to database
            try {
                chatLogRepository.save(chatLog);
            } catch (Exception e) {
                logger.error("Failed to save chat log: {}", e.getMessage());
            }
            
            // Store messages in session
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) session.get("messages");
            
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            userMessage.put("timestamp", new Date());
            messages.add(userMessage);
            
            Map<String, Object> aiMessage = new HashMap<>();
            aiMessage.put("role", "assistant");
            aiMessage.put("content", responseText);
            aiMessage.put("timestamp", new Date());
            messages.add(aiMessage);
            
            // Update session
            session.put("lastMessage", message);
            session.put("updatedAt", new Date());
            
            // Store in chat history (only if not anonymous)
            if (finalUserId != null && !finalUserId.equals("anonymous") && !finalUserId.equals("0")) {
                chatHistory.computeIfAbsent(finalUserId, k -> new ArrayList<>()).addAll(messages);
            }
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionId", finalSessionId);
            response.put("response", responseText);
            response.put("source", responseSource);
            response.put("fallback", !"AI".equals(responseSource) && !"DATABASE".equals(responseSource));
            if (foundProducts && !productIds.isEmpty()) {
                response.put("productIds", productIds);
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            
            // Try to save error log
            if (chatLog != null) {
                try {
                    chatLog.setAiResponse("Error: " + e.getMessage());
                    chatLog.setResponseSource("ERROR");
                    chatLogRepository.save(chatLog);
                } catch (Exception ex) {
                    logger.error("Failed to save error log: {}", ex.getMessage());
                }
            }
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Xin lỗi, tôi gặp lỗi khi xử lý tin nhắn của bạn. Vui lòng thử lại sau.");
            return errorResponse;
        }
    }
    
    /**
     * Check if question is product-related
     */
    private boolean isProductRelatedQuestion(String message) {
        String lowerMessage = message.toLowerCase().trim();
        
        // Product-related keywords - including detail/info queries
        String[] productKeywords = {
            "sản phẩm", "mua", "bán", "giá", "giá bao nhiêu", "có bán", "có không",
            "chi tiết", "thông tin", "mô tả", "giới thiệu", "đặc điểm", "tính năng",
            "thông số", "spec", "specification", "review", "đánh giá",
            "laptop", "điện thoại", "smartphone", "phone", "iphone", "samsung",
            "tai nghe", "headphone", "đồng hồ", "watch", "loa", "speaker",
            "chuột", "mouse", "bàn phím", "keyboard", "màn hình", "monitor",
            "máy tính", "pc", "tablet", "ipad", "macbook", "dell", "lenovo",
            "xiaomi", "oppo", "huawei", "sony", "lg", "asus", "acer", "hp"
        };
        
        for (String keyword : productKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Format product response from database data (NO AI - direct from DB)
     */
    private String formatProductResponseFromDatabase(List<Map<String, Object>> products, String userMessage) {
        StringBuilder response = new StringBuilder();
        
        if (products.size() == 1) {
            // Single product - detailed response with ALL information from DB
            Map<String, Object> product = products.get(0);
            String name = String.valueOf(product.getOrDefault("name", "Sản phẩm"));
            Object price = product.get("price");
            Object salePrice = product.getOrDefault("sale_price", price);
            Object rating = product.getOrDefault("average_rating", 0);
            Object stock = product.getOrDefault("stock_quantity", 0);
            Object reviewCount = product.getOrDefault("review_count", 0);
            String sku = String.valueOf(product.getOrDefault("sku", ""));
            String categoryName = String.valueOf(product.getOrDefault("category_name", ""));
            String brandName = String.valueOf(product.getOrDefault("brand_name", ""));
            String description = String.valueOf(product.getOrDefault("description", ""));
            if (description.length() > 200) {
                description = description.substring(0, 200) + "...";
            }
            
            // Check if user is asking for details specifically
            boolean isDetailQuery = userMessage.toLowerCase().contains("chi tiết") || 
                                   userMessage.toLowerCase().contains("thông tin") ||
                                   userMessage.toLowerCase().contains("mô tả");
            
            if (isDetailQuery) {
                response.append("📦 **").append(name).append("**\n\n");
            } else {
                response.append("Tôi tìm thấy sản phẩm phù hợp:\n\n");
                response.append("📦 **").append(name).append("**\n");
            }
            
            // Show brand and category from DB
            if (!brandName.isEmpty() && !brandName.equals("null")) {
                response.append("🏷️ **Thương hiệu:** ").append(brandName).append("\n");
            }
            if (!categoryName.isEmpty() && !categoryName.equals("null")) {
                response.append("📂 **Danh mục:** ").append(categoryName).append("\n");
            }
            if (!sku.isEmpty() && !sku.equals("null")) {
                response.append("🔖 **Mã sản phẩm:** ").append(sku).append("\n");
            }
            
            // Always show description for detail queries, or if available
            String fullDescription = String.valueOf(product.getOrDefault("description", ""));
            if (isDetailQuery && fullDescription.length() > 0 && !fullDescription.equals("null")) {
                // Show more description for detail queries (up to 500 chars)
                String detailDesc = fullDescription.length() > 500 ? 
                    fullDescription.substring(0, 500) + "..." : fullDescription;
                response.append("\n📝 **Mô tả:**\n").append(detailDesc).append("\n\n");
            } else if (description.length() > 0 && !description.equals("null")) {
                response.append("\n📝 ").append(description).append("\n");
            }
            
            if (salePrice != null && !salePrice.equals(price)) {
                response.append("\n💰 **Giá:** ").append(formatPrice(salePrice)).append(" VND");
                response.append(" (Giảm từ ").append(formatPrice(price)).append(" VND)\n");
            } else {
                response.append("\n💰 **Giá:** ").append(formatPrice(price)).append(" VND\n");
            }
            if (rating != null && !rating.equals(0)) {
                response.append("⭐ **Đánh giá:** ").append(rating).append("/5 sao");
                if (reviewCount != null && !reviewCount.equals(0)) {
                    response.append(" (").append(reviewCount).append(" đánh giá)");
                }
                response.append("\n");
            }
            response.append("📊 **Tồn kho:** ").append(stock).append(" sản phẩm\n");
            
            if (isDetailQuery) {
                response.append("\n💡 Bạn có thể xem thêm hình ảnh và đặt mua ngay trên trang sản phẩm!");
            } else {
                response.append("\nBạn có muốn xem thêm thông tin chi tiết không?");
            }
        } else {
            // Multiple products - check if it's a detail query
            boolean isDetailQuery = userMessage.toLowerCase().contains("chi tiết") || 
                                   userMessage.toLowerCase().contains("thông tin") ||
                                   userMessage.toLowerCase().contains("mô tả");
            
            if (isDetailQuery && products.size() <= 3) {
                // For detail queries with few results, show details for all with FULL DB info
                response.append("Tôi tìm thấy ").append(products.size()).append(" sản phẩm phù hợp:\n\n");
                for (int i = 0; i < products.size(); i++) {
                    Map<String, Object> product = products.get(i);
                    String name = String.valueOf(product.getOrDefault("name", "Sản phẩm"));
                    Object price = product.get("price");
                    Object salePrice = product.getOrDefault("sale_price", price);
                    Object rating = product.getOrDefault("average_rating", 0);
                    Object stock = product.getOrDefault("stock_quantity", 0);
                    Object reviewCount = product.getOrDefault("review_count", 0);
                    String sku = String.valueOf(product.getOrDefault("sku", ""));
                    String categoryName = String.valueOf(product.getOrDefault("category_name", ""));
                    String brandName = String.valueOf(product.getOrDefault("brand_name", ""));
                    String description = String.valueOf(product.getOrDefault("description", ""));
                    if (description.length() > 200) {
                        description = description.substring(0, 200) + "...";
                    }
                    
                    response.append("**").append(name).append("**\n");
                    if (!brandName.isEmpty() && !brandName.equals("null")) {
                        response.append("🏷️ Thương hiệu: ").append(brandName).append("\n");
                    }
                    if (!categoryName.isEmpty() && !categoryName.equals("null")) {
                        response.append("📂 Danh mục: ").append(categoryName).append("\n");
                    }
                    if (!sku.isEmpty() && !sku.equals("null")) {
                        response.append("🔖 Mã SP: ").append(sku).append("\n");
                    }
                    if (description.length() > 0 && !description.equals("null")) {
                        response.append("📝 ").append(description).append("\n");
                    }
                    if (salePrice != null && !salePrice.equals(price)) {
                        response.append("💰 Giá: ").append(formatPrice(salePrice)).append(" VND");
                        response.append(" (Giảm từ ").append(formatPrice(price)).append(" VND)\n");
                    } else {
                        response.append("💰 Giá: ").append(formatPrice(price)).append(" VND\n");
                    }
                    if (rating != null && !rating.equals(0)) {
                        response.append("⭐ Đánh giá: ").append(rating).append("/5");
                        if (reviewCount != null && !reviewCount.equals(0)) {
                            response.append(" (").append(reviewCount).append(" đánh giá)");
                        }
                        response.append("\n");
                    }
                    response.append("📊 Tồn kho: ").append(stock).append(" sản phẩm\n");
                    if (i < products.size() - 1) {
                        response.append("\n---\n\n");
                    }
                }
            } else {
                // Multiple products - list format with DB information
                response.append("Tôi tìm thấy ").append(products.size()).append(" sản phẩm phù hợp:\n\n");
                
                int count = 1;
                for (Map<String, Object> product : products) {
                    String name = String.valueOf(product.getOrDefault("name", "Sản phẩm"));
                    Object price = product.get("price");
                    Object salePrice = product.getOrDefault("sale_price", price);
                    Object rating = product.getOrDefault("average_rating", 0);
                    String brandName = String.valueOf(product.getOrDefault("brand_name", ""));
                    String categoryName = String.valueOf(product.getOrDefault("category_name", ""));
                    
                    response.append(count++).append(". **").append(name).append("**\n");
                    if (!brandName.isEmpty() && !brandName.equals("null")) {
                        response.append("   🏷️ ").append(brandName);
                        if (!categoryName.isEmpty() && !categoryName.equals("null")) {
                            response.append(" - ").append(categoryName);
                        }
                        response.append("\n");
                    }
                    if (salePrice != null && !salePrice.equals(price)) {
                        response.append("   💰 Giá: ").append(formatPrice(salePrice)).append(" VND");
                        response.append(" (Giảm từ ").append(formatPrice(price)).append(" VND)\n");
                    } else {
                        response.append("   💰 Giá: ").append(formatPrice(price)).append(" VND\n");
                    }
                    if (rating != null && !rating.equals(0)) {
                        response.append("   ⭐ ").append(rating).append("/5\n");
                    }
                    response.append("\n");
                }
                
                if (isDetailQuery) {
                    response.append("Vui lòng chỉ định rõ tên sản phẩm bạn muốn xem chi tiết (ví dụ: 'chi tiết iPhone 15 Pro').");
                } else {
                    response.append("Bạn muốn xem chi tiết sản phẩm nào?");
                }
            }
        }
        
        return response.toString();
    }
    
    /**
     * Extract keywords from user message - IMPROVED VERSION
     */
    private String[] extractKeywords(String message) {
        // Simple keyword extraction
        String lowerMessage = message.toLowerCase().trim();
        
        // Remove common question words that don't help with search
        String cleanedMessage = lowerMessage
            .replaceAll("\\b(chi tiết|thông tin|mô tả|giới thiệu|về|cho|tôi|bạn|có|không|gì|nào|đó|này)\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();
        
        // Common product keywords
        List<String> keywords = new ArrayList<>();
        
        // Product types - add both Vietnamese and English
        if (lowerMessage.contains("laptop") || lowerMessage.contains("máy tính")) {
            keywords.add("laptop");
            keywords.add("máy tính");
        }
        if (lowerMessage.contains("điện thoại") || lowerMessage.contains("smartphone") || lowerMessage.contains("phone")) {
            keywords.add("điện thoại");
            keywords.add("phone");
            keywords.add("smartphone");
        }
        if (lowerMessage.contains("tai nghe") || lowerMessage.contains("headphone")) {
            keywords.add("tai nghe");
            keywords.add("headphone");
        }
        if (lowerMessage.contains("đồng hồ") || lowerMessage.contains("watch")) {
            keywords.add("đồng hồ");
            keywords.add("watch");
        }
        if (lowerMessage.contains("loa") || lowerMessage.contains("speaker")) {
            keywords.add("loa");
            keywords.add("speaker");
        }
        if (lowerMessage.contains("chuột") || lowerMessage.contains("mouse")) {
            keywords.add("chuột");
            keywords.add("mouse");
        }
        if (lowerMessage.contains("bàn phím") || lowerMessage.contains("keyboard")) {
            keywords.add("bàn phím");
            keywords.add("keyboard");
        }
        if (lowerMessage.contains("màn hình") || lowerMessage.contains("monitor")) {
            keywords.add("màn hình");
            keywords.add("monitor");
        }
        
        // Brands - add both brand name and product name if mentioned
        if (lowerMessage.contains("iphone")) {
            keywords.add("iphone"); // Add "iphone" directly, not just "apple"
            keywords.add("apple");
        }
        if (lowerMessage.contains("apple") || lowerMessage.contains("mac") || lowerMessage.contains("macbook")) {
            keywords.add("apple");
            keywords.add("mac");
            keywords.add("macbook");
        }
        if (lowerMessage.contains("samsung") || lowerMessage.contains("galaxy")) {
            keywords.add("samsung");
            keywords.add("galaxy");
        }
        if (lowerMessage.contains("xiaomi") || lowerMessage.contains("mi ")) {
            keywords.add("xiaomi");
            keywords.add("mi");
        }
        if (lowerMessage.contains("oppo")) {
            keywords.add("oppo");
        }
        if (lowerMessage.contains("huawei")) {
            keywords.add("huawei");
        }
        if (lowerMessage.contains("lenovo")) {
            keywords.add("lenovo");
        }
        if (lowerMessage.contains("dell")) {
            keywords.add("dell");
        }
        if (lowerMessage.contains("asus")) {
            keywords.add("asus");
        }
        if (lowerMessage.contains("acer")) {
            keywords.add("acer");
        }
        if (lowerMessage.contains("hp")) {
            keywords.add("hp");
        }
        
        // Also extract any words that look like product names (capitalized words, numbers)
        // This helps catch queries like "iPhone 15 Pro", "MacBook Pro 14", etc.
        String[] words = cleanedMessage.split("\\s+");
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (cleanWord.length() >= 2) { // Reduced from 3 to 2 to catch more keywords
                // Check if word contains numbers (like "15", "14", "pro", etc.)
                if (cleanWord.matches(".*\\d+.*") || 
                    cleanWord.matches("pro|max|plus|mini|ultra|premium|standard|gaming|office")) {
                    keywords.add(cleanWord);
                } else if (cleanWord.length() >= 3) {
                    // Add any meaningful word (not common stop words)
                    keywords.add(cleanWord);
                }
            }
        }
        
        // For detail queries, also try to extract product name as a phrase
        // This helps with queries like "chi tiết iPhone 15 Pro" -> extract "iphone 15 pro"
        if (lowerMessage.contains("chi tiết") || lowerMessage.contains("thông tin") || lowerMessage.contains("mô tả")) {
            // Try to extract the product name part after "chi tiết", "thông tin", "mô tả"
            String[] detailPrefixes = {"chi tiết", "thông tin", "mô tả", "giới thiệu", "về"};
            for (String prefix : detailPrefixes) {
                if (lowerMessage.contains(prefix)) {
                    int prefixIndex = lowerMessage.indexOf(prefix);
                    String afterPrefix = lowerMessage.substring(prefixIndex + prefix.length()).trim();
                    // Remove common words
                    afterPrefix = afterPrefix
                        .replaceAll("\\b(sản phẩm|điện thoại|laptop|máy tính|đi|thì|nhé|nhá)\\b", "")
                        .trim();
                    if (afterPrefix.length() >= 3) {
                        // Add the whole phrase as a keyword
                        keywords.add(afterPrefix);
                        // Also add individual words
                        String[] phraseWords = afterPrefix.split("\\s+");
                        for (String phraseWord : phraseWords) {
                            String cleanPhraseWord = phraseWord.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                            if (cleanPhraseWord.length() >= 2) {
                                keywords.add(cleanPhraseWord);
                            }
                        }
                        break; // Only process first matching prefix
                    }
                }
            }
        }
        
        // If no keywords found, try using the original message (fallback)
        if (keywords.isEmpty() && !cleanedMessage.isEmpty()) {
            String[] fallbackWords = cleanedMessage.split("\\s+");
            for (String word : fallbackWords) {
                String cleanWord = word.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleanWord.length() >= 2) {
                    keywords.add(cleanWord);
                }
            }
        }
        
        // Remove duplicates
        keywords = keywords.stream().distinct().collect(Collectors.toList());
        
        logger.debug("Extracted keywords from '{}': {}", message, keywords);
        
        return keywords.toArray(new String[0]);
    }
    
    /**
     * Format price
     */
    private String formatPrice(Object price) {
        if (price == null) return "0";
        try {
            double p = Double.parseDouble(price.toString());
            return String.format("%.0f", p);
        } catch (Exception e) {
            return price.toString();
        }
    }
    
    /**
     * Get chat history for a user
     */
    public List<Map<String, Object>> getChatHistory(String userId) {
        return chatHistory.getOrDefault(userId, new ArrayList<>());
    }
    
    /**
     * Get chat sessions for a user
     */
    public List<Map<String, Object>> getChatSessions(String userId) {
        return chatSessions.values().stream()
            .filter(session -> userId.equals(session.get("userId")))
            .toList();
    }
    
    /**
     * Create new chat session
     */
    public Map<String, Object> createChatSession(String userId, String title) {
        String sessionId = UUID.randomUUID().toString();
        Map<String, Object> session = new HashMap<>();
        session.put("sessionId", sessionId);
        session.put("userId", userId);
        session.put("title", title);
        session.put("createdAt", new Date());
        session.put("messages", new ArrayList<>());
        
        chatSessions.put(sessionId, session);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", sessionId);
        response.put("message", "Chat session created successfully");
        
        return response;
    }
    
    /**
     * End chat session
     */
    public Map<String, Object> endChatSession(String sessionId) {
        chatSessions.remove(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Chat session ended successfully");
        
        return response;
    }
    
    /**
     * Get product recommendations based on chat context - USE DATABASE DIRECTLY
     */
    public List<Map<String, Object>> getChatProductRecommendations(String sessionId, String query) {
        try {
            // Get session to analyze conversation context
            Map<String, Object> session = chatSessions.get(sessionId);
            
            if (session == null) {
                logger.warn("Session not found: {}", sessionId);
                return getDefaultProductRecommendations(query);
            }
            
            // Extract keywords from conversation
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) session.get("messages");
            String conversationContext = extractKeywordsFromConversation(messages);
            
            // Combine with current query
            String searchQuery = query != null ? query + " " + conversationContext : conversationContext;
            
            // Search for products directly from database
            List<Map<String, Object>> products = productRepository.searchProducts(searchQuery, 10);
            
            if (!products.isEmpty()) {
                // Add relevance score based on conversation context
                products.forEach(product -> {
                    double relevance = calculateRelevance(product, conversationContext);
                    product.put("relevanceScore", relevance);
                    product.put("recommendationReason", generateRecommendationReason(product, conversationContext));
                });
                
                // Sort by relevance
                products.sort((p1, p2) -> {
                    Double score1 = (Double) p1.getOrDefault("relevanceScore", 0.0);
                    Double score2 = (Double) p2.getOrDefault("relevanceScore", 0.0);
                    return score2.compareTo(score1);
                });
                
                logger.info("Found {} product recommendations for session {}", products.size(), sessionId);
                return products;
            }
            
        } catch (Exception e) {
            logger.error("Error getting product recommendations: {}", e.getMessage(), e);
        }
        
        return getDefaultProductRecommendations(query);
    }
    
    /**
     * Extract keywords from conversation history
     */
    private String extractKeywordsFromConversation(List<Map<String, Object>> messages) {
        StringBuilder keywords = new StringBuilder();
        
        // Get last 5 messages for context
        int start = Math.max(0, messages.size() - 5);
        for (int i = start; i < messages.size(); i++) {
            Map<String, Object> message = messages.get(i);
            String content = (String) message.get("content");
            if (content != null) {
                keywords.append(content).append(" ");
            }
        }
        
        return keywords.toString().trim();
    }
    
    /**
     * Calculate relevance score for product based on conversation context
     */
    private double calculateRelevance(Map<String, Object> product, String context) {
        double score = 0.5; // Base score
        
        String productName = String.valueOf(product.get("name")).toLowerCase();
        String productDescription = String.valueOf(product.getOrDefault("description", "")).toLowerCase();
        String contextLower = context.toLowerCase();
        
        // Check if product name appears in conversation
        if (contextLower.contains(productName)) {
            score += 0.3;
        }
        
        // Check for keyword matches
        String[] keywords = {"laptop", "điện thoại", "phone", "máy tính", "gaming", "văn phòng", 
                           "giá rẻ", "cao cấp", "premium", "budget"};
        for (String keyword : keywords) {
            if (contextLower.contains(keyword) && 
                (productName.contains(keyword) || productDescription.contains(keyword))) {
                score += 0.1;
            }
        }
        
        // Cap at 1.0
        return Math.min(score, 1.0);
    }
    
    /**
     * Generate recommendation reason
     */
    private String generateRecommendationReason(Map<String, Object> product, String context) {
        String contextLower = context.toLowerCase();
        
        if (contextLower.contains("gaming")) {
            return "Phù hợp cho gaming dựa trên yêu cầu của bạn";
        } else if (contextLower.contains("văn phòng") || contextLower.contains("work")) {
            return "Phù hợp cho công việc văn phòng";
        } else if (contextLower.contains("giá rẻ") || contextLower.contains("budget")) {
            return "Lựa chọn tốt trong tầm giá";
        } else {
            return "Sản phẩm phổ biến được nhiều người quan tâm";
        }
    }
    
    /**
     * Get default product recommendations when no session found - USE DATABASE DIRECTLY
     */
    private List<Map<String, Object>> getDefaultProductRecommendations(String query) {
        try {
            String searchQuery = query != null ? query : "laptop";
            // Search directly from database
            List<Map<String, Object>> products = productRepository.searchProducts(searchQuery, 5);
            if (!products.isEmpty()) {
                return products;
            }
        } catch (Exception e) {
            logger.error("Error getting default recommendations: {}", e.getMessage());
        }
        
        return new ArrayList<>();
    }

    /**
     * Simple rule-based responses for common small-talk or fallback scenarios
     */
    private String generateRuleBasedResponse(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return null;
        }

        if (containsAny(normalized, "xin chào", "chào bạn", "hello", "hi", "alo")) {
            return "Xin chào! Mình là Stylist AI của cửa hàng. Bạn đang quan tâm tới dòng sản phẩm nào? " +
                "Mình có thể gợi ý laptop, điện thoại, phụ kiện… chỉ cần bạn cho biết nhu cầu.";
        }

        if (containsAny(normalized, "cảm ơn", "thank", "thanks")) {
            return "Rất vui được hỗ trợ bạn! Nếu bạn còn câu hỏi nào khác hoặc cần tư vấn thêm về sản phẩm, cứ nhắn cho mình nhé.";
        }

        if (containsAny(normalized, "giúp", "hỗ trợ", "support", "tư vấn")) {
            return "Mình có thể giúp bạn tìm sản phẩm phù hợp theo ngân sách, nhu cầu chơi game, làm việc hay quà tặng. " +
                "Bạn mô tả sơ qua nhu cầu để mình tư vấn nhé!";
        }

        if (containsAny(normalized, "giờ mở cửa", "làm việc", "shipping", "giao hàng", "bao lâu")) {
            return "Cửa hàng giao hàng toàn quốc, thời gian từ 1-5 ngày tùy khu vực. " +
                "Bạn có thể đặt online và theo dõi tình trạng đơn ngay trong tài khoản của mình.";
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
