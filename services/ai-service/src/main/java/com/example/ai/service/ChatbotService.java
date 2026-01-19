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

    // Session keys
    private static final String SESSION_LAST_PRODUCTS = "lastProducts";
    private static final String SESSION_LAST_PRODUCTS_AT = "lastProductsAt";
    
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

            // Early small-talk routing (do not send to product search / Gemini)
            // 1) Greetings: enforce the desired shop greeting template consistently
            if (isGreeting(message != null ? message : "")) {
                String responseText = "Xin chào, tôi là AI tư vấn công nghệ. Bạn đang tìm sản phẩm công nghệ nào?";
                return respondEarlyRuleBased(finalUserId, finalSessionId, session, chatLog, message, responseText, "GREETING");
            }

            // 2) Simple math: answer directly (helps when Gemini is unavailable and avoids 'giúp' -> product help)
            String mathAnswer = trySolveSimpleMath(message != null ? message : "");
            if (mathAnswer != null) {
                return respondEarlyRuleBased(finalUserId, finalSessionId, session, chatLog, message, mathAnswer, "MATH");
            }

            // Fast-path: user confirms "có" after a list -> ask which product (avoid falling into AI fallback)
            if (isSimpleAffirmation(message != null ? message : "") && hasLastProducts(session)) {
                String responseText = "Bạn muốn xem chi tiết sản phẩm nào?\n" +
                    "- Bạn có thể trả lời theo số thứ tự (ví dụ: \"số 3\")\n" +
                    "- Hoặc gõ tên sản phẩm (ví dụ: \"MacBook Pro 16-inch (M4 Max)\")";

                chatLog.setIsProductRelated(true);
                chatLog.setAiResponse(responseText);
                chatLog.setUsedAI(false);
                chatLog.setFoundProducts(true);
                chatLog.setResponseSource("ASK_FOR_PRODUCT_SELECTION");

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

                session.put("lastMessage", message);
                session.put("updatedAt", new Date());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("sessionId", finalSessionId);
                response.put("response", responseText);
                response.put("source", "ASK_FOR_PRODUCT_SELECTION");
                response.put("fallback", false);
                return response;
            }
            
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
                List<Map<String, Object>> foundProductsList = new ArrayList<>();
                boolean isDetailQuery = isDetailQuery(message);

                // Sale intent: use on-sale products directly instead of keyword search
                if (isSaleQuery(message)) {
                    try {
                        foundProductsList = productRepository.getProductsOnSale(10);
                    } catch (Exception e) {
                        logger.warn("Failed to get products on sale: {}", e.getMessage());
                    }
                }

                // Strategy - for detail queries, first try to resolve from the last shown list in this session
                if (foundProductsList.isEmpty() && (isDetailQuery || isSelectionQuery(message))) {
                    Optional<Map<String, Object>> fromSession = resolveDetailProductFromSession(session, message);
                    if (fromSession.isPresent()) {
                        Map<String, Object> chosen = fromSession.get();
                        Object idObj = chosen.get("id");
                        if (idObj != null) {
                            try {
                                Long pid = Long.valueOf(String.valueOf(idObj));
                                Map<String, Object> full = productRepository.getProductFullById(pid);
                                foundProductsList = new ArrayList<>();
                                foundProductsList.add(full != null ? full : chosen);
                                logger.info("Detail query resolved from session lastProducts: {}", chosen.get("name"));
                            } catch (Exception e) {
                                // fallback to chosen as-is
                                foundProductsList = new ArrayList<>();
                                foundProductsList.add(chosen);
                                logger.warn("Failed to load full product by id from session selection: {}", e.getMessage());
                            }
                        }
                    }
                }

                String[] keywords = extractKeywords(message);
                String detailPhrase = isDetailQuery ? extractDetailProductPhrase(message) : null;
                
                try {
                    logger.info("Searching products for message: '{}' with keywords: {}", message, Arrays.toString(keywords));
                    
                    // Strategy 0: If it's a detail query and we have a phrase (e.g. "google pixel 9"),
                    // search by that phrase first and try to pick ONE best match (avoid keyword-by-keyword broadening).
                    if (foundProductsList.isEmpty() && isDetailQuery && StringUtils.hasText(detailPhrase)) {
                        try {
                            logger.debug("Detail query - searching by extracted phrase: '{}'", detailPhrase);
                            List<Map<String, Object>> phraseResults = productRepository.searchProducts(detailPhrase, 20);
                            Optional<Map<String, Object>> best = resolveBestMatchByName(phraseResults, detailPhrase);
                            if (best.isPresent()) {
                                Map<String, Object> chosen = best.get();
                                foundProductsList = new ArrayList<>();
                                foundProductsList.add(loadFullProductIfPossible(chosen));
                                logger.info("Detail query best-match selected: {}", chosen.get("name"));
                            } else if (phraseResults != null && !phraseResults.isEmpty()) {
                                // If no best single match, keep top few but don't explode into unrelated results
                                foundProductsList = phraseResults.subList(0, Math.min(3, phraseResults.size()));
                            }
                        } catch (Exception e) {
                            logger.warn("Detail phrase search failed: {}", e.getMessage());
                        }
                    }

                    // If still empty for detail query, try a shorter phrase (e.g. last 2 tokens) before keyword search.
                    if (foundProductsList.isEmpty() && isDetailQuery && StringUtils.hasText(detailPhrase)) {
                        String shortened = shortenPhrase(detailPhrase);
                        if (StringUtils.hasText(shortened) && !shortened.equalsIgnoreCase(detailPhrase)) {
                            try {
                                logger.debug("Detail query - searching by shortened phrase: '{}'", shortened);
                                List<Map<String, Object>> shortResults = productRepository.searchProducts(shortened, 20);
                                Optional<Map<String, Object>> best = resolveBestMatchByName(shortResults, detailPhrase);
                                if (best.isPresent()) {
                                    Map<String, Object> chosen = best.get();
                                    foundProductsList = new ArrayList<>();
                                    foundProductsList.add(loadFullProductIfPossible(chosen));
                                    logger.info("Detail query best-match (shortened) selected: {}", chosen.get("name"));
                                } else if (shortResults != null && !shortResults.isEmpty()) {
                                    foundProductsList = shortResults.subList(0, Math.min(3, shortResults.size()));
                                }
                            } catch (Exception e) {
                                logger.warn("Detail shortened phrase search failed: {}", e.getMessage());
                            }
                        }
                    }
                    
                    // Strategy 1: Search with each keyword individually (more flexible)
                    // For detail queries, only do keyword-by-keyword search as a LAST resort because it tends to pull
                    // unrelated products (e.g. searching "google" returns watch, etc.).
                    if (foundProductsList.isEmpty() && keywords.length > 0) {
                        for (String keyword : keywords) {
                            // Avoid noisy keywords for detail queries (numbers-only, too short)
                            if (isDetailQuery) {
                                if (keyword == null) continue;
                                String k = keyword.trim();
                                if (k.length() < 3) continue;
                                if (k.matches("\\d+")) continue;
                            }
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
                    if (foundProductsList.isEmpty() && message != null && message.length() > 0) {
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
                    // If user asked specifically for a brand (e.g. "của apple"), keep results on-brand
                    String brandFilter = extractBrandFilter(message);
                    if (StringUtils.hasText(brandFilter)) {
                        List<Map<String, Object>> filtered = foundProductsList.stream()
                            .filter(p -> normalizeForMatch(String.valueOf(p.getOrDefault("brand_name", ""))).contains(brandFilter))
                            .toList();
                        if (!filtered.isEmpty()) {
                            foundProductsList = new ArrayList<>(filtered);
                        } else {
                            // Explicit brand asked but no matching products -> respond clearly
                            responseText = "Xin lỗi, hiện tại shop chưa hỗ trợ hoặc không có sản phẩm của thương hiệu này.";
                            responseSource = "NO_BRAND_PRODUCTS";
                            usedAI = false;
                            foundProducts = false;
                            // Skip formatting, proceed to logging/response block
                            chatLog.setAiResponse(responseText);
                            chatLog.setUsedAI(false);
                            chatLog.setFoundProducts(false);
                            chatLog.setResponseSource(responseSource);
                            try { chatLogRepository.save(chatLog); } catch (Exception e) { logger.error("Failed to save chat log: {}", e.getMessage()); }
                            Map<String, Object> response = new HashMap<>();
                            response.put("success", true);
                            response.put("sessionId", finalSessionId);
                            response.put("response", responseText);
                            response.put("source", responseSource);
                            response.put("fallback", false);
                            return response;
                        }
                    }

                    // Soft brand preference: if user mentions a brand (even without "của ..."),
                    // prefer keeping on-brand results when available (prevents "13"/"plus" pulling unrelated items).
                    String softBrand = extractSoftBrandPreference(message);
                    if (StringUtils.hasText(softBrand)) {
                        List<Map<String, Object>> softFiltered = foundProductsList.stream()
                            .filter(p -> normalizeForMatch(String.valueOf(p.getOrDefault("brand_name", ""))).contains(softBrand)
                                || normalizeForMatch(String.valueOf(p.getOrDefault("name", ""))).contains(softBrand))
                            .toList();
                        if (!softFiltered.isEmpty()) {
                            foundProductsList = new ArrayList<>(softFiltered);
                        }
                    }

                    // Product-line preference (e.g. user asks specifically for "iPhone" -> only iPhone models).
                    String productLine = extractProductLinePreference(message);
                    if (StringUtils.hasText(productLine)) {
                        List<Map<String, Object>> byLine = foundProductsList.stream()
                            .filter(p -> normalizeForMatch(String.valueOf(p.getOrDefault("name", ""))).contains(productLine))
                            .toList();
                        if (!byLine.isEmpty()) {
                            foundProductsList = new ArrayList<>(byLine);
                        }
                    }

                    // Category preference (e.g. "điện thoại/phone" should not return watches/headphones).
                    String categoryPref = extractCategoryPreference(message);
                    if (StringUtils.hasText(categoryPref)) {
                        List<Map<String, Object>> byCategory = foundProductsList.stream()
                            .filter(p -> normalizeForMatch(String.valueOf(p.getOrDefault("category_name", ""))).contains(categoryPref))
                            .toList();
                        if (!byCategory.isEmpty()) {
                            foundProductsList = new ArrayList<>(byCategory);
                        }
                    }

                    // Step 3: Format response from database data (NO AI)
                    foundProducts = true;
                    // For selection queries like "có, sản phẩm này đi <name>", force detail-style formatting
                    String formatMessage = isDetailQuery || isSelectionQuery(message) ? ("chi tiết " + message) : message;
                    responseText = formatProductResponseFromDatabase(foundProductsList, formatMessage);
                    responseSource = "DATABASE";
                    usedAI = false;

                    // Remember the last shown list for follow-up "chi tiết ..." questions
                    storeLastProductsInSession(session, foundProductsList);
                    
                    // Collect product IDs and names for logging
                    for (Map<String, Object> product : foundProductsList) {
                        Object id = product.get("id");
                        Object name = product.get("name");
                        if (id != null) productIds.add(String.valueOf(id));
                        if (name != null) productNames.add(String.valueOf(name));
                    }
                } else {
                    // Step 4: No products found - check if it's a generic detail query
                    String safeMsgLower = message != null ? message.toLowerCase() : "";
                    boolean isGenericDetailQuery = safeMsgLower.matches(".*(chi tiết|thông tin|mô tả).*sản phẩm.*") &&
                                                  !safeMsgLower.matches(".*(laptop|điện thoại|phone|iphone|samsung|xiaomi|oppo|huawei|dell|lenovo|asus|acer|hp|macbook|ipad|tablet|tai nghe|headphone|đồng hồ|watch|loa|speaker|chuột|mouse|bàn phím|keyboard|màn hình|monitor).*");
                    
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
                                    String categoryName = String.valueOf(product.getOrDefault("category_name", ""));
                                    Object price = product.get("price");
                                    Object salePrice = product.getOrDefault("sale_price", price);
                                    suggestResponse.append(count++).append(". ").append(name);
                                    if (salePrice != null) {
                                        suggestResponse.append(" - ").append(formatPrice(salePrice, categoryName)).append(" VND");
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

    private Map<String, Object> respondEarlyRuleBased(
        String userId,
        String sessionId,
        Map<String, Object> session,
        ChatLog chatLog,
        String userMessageText,
        String responseText,
        String tag
    ) {
        // Update chat log
        if (chatLog != null) {
            chatLog.setIsProductRelated(false);
            chatLog.setAiResponse(responseText);
            chatLog.setUsedAI(false);
            chatLog.setFoundProducts(false);
            chatLog.setResponseSource("RULE_" + tag);
            try {
                chatLogRepository.save(chatLog);
            } catch (Exception e) {
                logger.error("Failed to save chat log: {}", e.getMessage());
            }
        }

        // Store messages in session
        if (session != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) session.get("messages");
            if (messages != null) {
                Map<String, Object> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", userMessageText);
                userMsg.put("timestamp", new Date());
                messages.add(userMsg);

                Map<String, Object> aiMsg = new HashMap<>();
                aiMsg.put("role", "assistant");
                aiMsg.put("content", responseText);
                aiMsg.put("timestamp", new Date());
                messages.add(aiMsg);
            }
            session.put("lastMessage", userMessageText);
            session.put("updatedAt", new Date());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", sessionId);
        response.put("response", responseText);
        response.put("source", "RULE_" + tag);
        response.put("fallback", false);
        return response;
    }
    
    /**
     * Check if question is product-related
     */
    private boolean isProductRelatedQuestion(String message) {
        if (message == null) {
            return false;
        }

        // Normalize (lowercase + remove accents) so queries like "chi tiet" and "chi tiết" behave the same
        String normalized = normalizeForMatch(message);

        // Greetings should NOT be treated as product queries (avoid "bạn" -> "ban" confusion)
        if (isGreeting(message)) {
            return false;
        }
        
        // Product-related keywords - including detail/info queries
        String[] productKeywords = {
            // Generic commerce / product intents
            "san pham", "mua", "gia", "gia bao nhieu", "co ban", "co khong",
            // Detail/info intents (both accented + non-accented collapse here)
            "chi tiet", "thong tin", "mo ta", "gioi thieu", "dac diem", "tinh nang",
            "thong so", "spec", "specification", "review", "danh gia",
            // Categories / types
            "laptop", "dien thoai", "smartphone", "phone", "iphone", "samsung",
            "tai nghe", "headphone", "dong ho", "watch", "loa", "speaker",
            "chuot", "mouse", "ban phim", "keyboard", "man hinh", "monitor",
            "may tinh", "pc", "tablet", "ipad", "macbook", "dell", "lenovo",
            // Brands
            "xiaomi", "oppo", "huawei", "sony", "lg", "asus", "acer", "hp", "google", "pixel"
        };
        
        // Use token matching for 1-word keywords to avoid substring false-positives
        Set<String> tokens = new HashSet<>(Arrays.asList(normalized.split("\\s+")));
        for (String keyword : productKeywords) {
            String k = keyword.trim();
            if (k.isEmpty()) continue;
            if (k.contains(" ")) {
                if (normalized.contains(k)) return true;
            } else {
                if (tokens.contains(k)) return true;
            }
        }
        
        return false;
    }

    private boolean isGreeting(String message) {
        if (message == null) return false;
        String norm = normalizeForMatch(message);
        // Whole-message greetings or greetings at start
        if (norm.isEmpty()) return false;
        if (norm.equals("xin chao") || norm.equals("chao") || norm.equals("chao ban") || norm.equals("hello") || norm.equals("hi")) {
            return true;
        }
        return norm.startsWith("xin chao") || norm.startsWith("chao ");
    }

    private boolean isDetailQuery(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("chi tiết") || lower.contains("chi tiet") ||
            lower.contains("thông tin") || lower.contains("thong tin") ||
            lower.contains("mô tả") || lower.contains("mo ta");
    }

    private boolean isSaleQuery(String message) {
        if (message == null) return false;
        String norm = normalizeForMatch(message);
        return norm.contains("sale") || norm.contains("giam gia") || norm.contains("khuyen mai") || norm.contains("flash sale");
    }

    private boolean isSelectionQuery(String message) {
        if (message == null) return false;
        String norm = normalizeForMatch(message);
        // user picks an item from the just-listed results without explicitly saying "chi tiết"
        return norm.contains("san pham nay") || norm.contains("mau nay") || norm.contains("cai nay") ||
            norm.contains("chon") || norm.contains("lay") || norm.contains("cho xem") || norm.contains("mo ta");
    }

    private boolean isSimpleAffirmation(String message) {
        if (message == null) return false;
        String norm = normalizeForMatch(message);
        return norm.equals("co") || norm.equals("ok") || norm.equals("oke") || norm.equals("yes") || norm.equals("dong y");
    }

    private boolean hasLastProducts(Map<String, Object> session) {
        if (session == null) return false;
        Object lastObj = session.get(SESSION_LAST_PRODUCTS);
        return (lastObj instanceof List<?>) && !((List<?>) lastObj).isEmpty();
    }

    private String extractBrandFilter(String message) {
        if (message == null) return null;
        String norm = normalizeForMatch(message);
        // Require phrasing like "của <brand>" to avoid over-filtering for generic queries
        if (!(norm.contains("cua") || norm.contains("hang") || norm.contains("thuong hieu"))) {
            return null;
        }
        // OnePlus: handle "oneplus" and "one plus"
        if (norm.contains("oneplus") || norm.contains("one plus") || norm.contains("1+")) return "oneplus";
        if (norm.contains("apple")) return "apple";
        if (norm.contains("samsung")) return "samsung";
        if (norm.contains("xiaomi")) return "xiaomi";
        if (norm.contains("oppo")) return "oppo";
        if (norm.contains("huawei")) return "huawei";
        if (norm.contains("google")) return "google";
        if (norm.contains("sony")) return "sony";
        if (norm.contains("lenovo")) return "lenovo";
        if (norm.contains("dell")) return "dell";
        if (norm.contains("asus")) return "asus";
        if (norm.contains("hp")) return "hp";
        return null;
    }

    /**
     * Extract a brand that is mentioned anywhere in the message, even without "của ...".
     * This is used as a soft preference to keep results relevant.
     *
     * For now we keep this conservative and only cover the OnePlus family to fix "one plus" -> "one"+"plus" noise.
     */
    private String extractSoftBrandPreference(String message) {
        if (message == null) return null;
        String norm = normalizeForMatch(message);
        if (!StringUtils.hasText(norm)) return null;

        // Detect OnePlus mentions: "oneplus", "one plus", "1+"
        if (norm.contains("oneplus") || norm.contains("one plus") || norm.contains("1+")) {
            return "oneplus";
        }
        // Detect Apple/iPhone mention
        if (norm.contains("iphone") || norm.contains("apple")) {
            return "apple";
        }
        return null;
    }

    /**
     * Detect a specific product-line the user asked for (stronger than brand).
     * Example: "điện thoại iphone" -> "iphone".
     */
    private String extractProductLinePreference(String message) {
        if (message == null) return null;
        String norm = normalizeForMatch(message);
        if (!StringUtils.hasText(norm)) return null;
        if (norm.contains("iphone")) return "iphone";
        return null;
    }

    /**
     * Detect a category the user asked for, so we don't return accessories when user asked for phones, etc.
     * Returns a normalized token suitable for `contains()` against normalized `category_name`.
     */
    private String extractCategoryPreference(String message) {
        if (message == null) return null;
        String norm = normalizeForMatch(message);
        if (!StringUtils.hasText(norm)) return null;

        // Phone intent
        if (norm.contains("dien thoai") || norm.contains("phone") || norm.contains("smartphone")) {
            // category_name in DB is often "Phones" -> normalize => "phones"
            return "phone";
        }
        // Laptop intent
        if (norm.contains("laptop") || norm.contains("macbook")) {
            return "laptop";
        }
        // Headphones intent
        if (norm.contains("tai nghe") || norm.contains("headphone")) {
            return "headphone";
        }
        // Smartwatch intent
        if (norm.contains("dong ho") || norm.contains("watch")) {
            return "watch";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> resolveDetailProductFromSession(Map<String, Object> session, String message) {
        if (session == null || message == null) return Optional.empty();
        Object lastObj = session.get(SESSION_LAST_PRODUCTS);
        if (!(lastObj instanceof List<?>)) return Optional.empty();

        List<Map<String, Object>> lastProducts = (List<Map<String, Object>>) lastObj;
        if (lastProducts.isEmpty()) return Optional.empty();

        // 1) Try index selection: "sản phẩm số 4", "item 4", "#4"
        Optional<Map<String, Object>> byIndex = resolveByIndex(lastProducts, message);
        if (byIndex.isPresent()) return byIndex;

        // 2) Try name selection from the message
        String phrase = extractDetailProductPhrase(message);
        if (StringUtils.hasText(phrase)) {
            Optional<Map<String, Object>> byName = resolveBestMatchByName(lastProducts, phrase);
            if (byName.isPresent()) return byName;
        }

        // 3) If user typed the product name directly (e.g. "có, sản phẩm này đi MacBook Pro 16-inch..."),
        // match against last list by scanning the whole message.
        String normMessage = normalizeForMatch(message);
        Optional<Map<String, Object>> direct = resolveBestMatchByName(lastProducts, normMessage);
        if (direct.isPresent()) return direct;

        return Optional.empty();
    }

    private Optional<Map<String, Object>> resolveByIndex(List<Map<String, Object>> products, String message) {
        if (products == null || products.isEmpty() || message == null) return Optional.empty();
        String lower = message.toLowerCase(Locale.ROOT).trim();

        // Avoid treating model numbers (e.g. "pixel 9") as an index selection.
        // Only allow index selection when the user includes an explicit selector token.
        boolean hasSelectorToken = lower.contains("số") || lower.contains("#") ||
            lower.contains("sản phẩm") || lower.contains("sp ") || lower.contains("item") || lower.contains("mục");
        if (!hasSelectorToken) return Optional.empty();

        // Matches: "số 4", "sản phẩm số 4", "sp 4", "item #4", "#4"
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:\\b(?:sản\\s*phẩm|sp|item|mục)\\b\\s*)?(?:#\\s*)?(?:\\bsố\\b\\s*)?(\\d{1,2})\\b")
            .matcher(lower);
        if (!m.find()) return Optional.empty();

        try {
            int idx = Integer.parseInt(m.group(1));
            if (idx >= 1 && idx <= products.size()) {
                return Optional.ofNullable(products.get(idx - 1));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private Map<String, Object> loadFullProductIfPossible(Map<String, Object> product) {
        if (product == null) return null;
        Object idObj = product.get("id");
        if (idObj == null) return product;
        try {
            Long pid = Long.valueOf(String.valueOf(idObj));
            Map<String, Object> full = productRepository.getProductFullById(pid);
            return full != null ? full : product;
        } catch (Exception e) {
            return product;
        }
    }

    private void storeLastProductsInSession(Map<String, Object> session, List<Map<String, Object>> products) {
        if (session == null || products == null) return;
        // Store a shallow copy to avoid accidental mutation
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> p : products) {
            if (p != null) copy.add(p);
        }
        session.put(SESSION_LAST_PRODUCTS, copy);
        session.put(SESSION_LAST_PRODUCTS_AT, new Date());
    }

    /**
     * Extract the product phrase the user is referring to in a detail request.
     * Example: "chi tiết về google pixel 9 đi" -> "google pixel 9"
     */
    private String extractDetailProductPhrase(String message) {
        if (message == null) return null;
        // Work on normalized (no accents) to support both "chi tiết" and "chi tiet"
        String norm = normalizeForMatch(message);

        String[] prefixes = {"chi tiet", "thong tin", "mo ta", "gioi thieu", "ve"};
        for (String prefix : prefixes) {
            int idx = norm.indexOf(prefix);
            if (idx >= 0) {
                String after = norm.substring(idx + prefix.length()).trim();
                after = after
                    // Remove common filler words that often wrap the product name
                    .replaceAll("\\b(ve|san pham|dien thoai|laptop|may tinh|cho toi|giup toi|di|thi|nhe|nha|voi)\\b", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
                if (after.length() >= 3) {
                    return after;
                }
            }
        }

        return null;
    }

    private String shortenPhrase(String phrase) {
        if (!StringUtils.hasText(phrase)) return phrase;
        String[] parts = phrase.trim().split("\\s+");
        if (parts.length <= 2) return phrase.trim();
        // Keep last 2 tokens (often model + version), e.g. "pixel 9"
        return (parts[parts.length - 2] + " " + parts[parts.length - 1]).trim();
    }

    private Optional<Map<String, Object>> resolveBestMatchByName(List<Map<String, Object>> candidates, String phrase) {
        if (candidates == null || candidates.isEmpty() || !StringUtils.hasText(phrase)) return Optional.empty();

        String target = normalizeForMatch(phrase);
        if (!StringUtils.hasText(target)) return Optional.empty();

        Map<String, Object> best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Map<String, Object> p : candidates) {
            if (p == null) continue;
            String name = String.valueOf(p.getOrDefault("name", ""));
            String normName = normalizeForMatch(name);
            if (!StringUtils.hasText(normName)) continue;

            int score = scoreNameMatch(normName, target);
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }

        // Require a minimum confidence score to avoid random picks
        if (best != null && bestScore >= 60) {
            return Optional.of(best);
        }
        return Optional.empty();
    }

    private int scoreNameMatch(String normName, String target) {
        // Highest: exact match
        if (normName.equals(target)) return 100;
        // Next: contains target (e.g. "google pixel 9" in "google pixel 9 (128gb)")
        if (normName.contains(target)) return 90 - Math.min(20, (normName.length() - target.length()));
        // Next: target contains name (rare)
        if (target.contains(normName)) return 75 - Math.min(20, (target.length() - normName.length()));

        // Token containment: all target tokens included in name
        String[] tokens = target.split(" ");
        boolean all = true;
        for (String t : tokens) {
            if (t.length() >= 2 && !normName.contains(t)) {
                all = false;
                break;
            }
        }
        if (all) return 70;

        // Partial token overlap
        int overlap = 0;
        for (String t : tokens) {
            if (t.length() >= 2 && normName.contains(t)) overlap++;
        }
        return 40 + overlap * 5;
    }

    private String normalizeForMatch(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT);
        // Remove accents for Vietnamese-friendly matching
        String noAccents = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Keep letters/numbers/spaces only
        return noAccents
            .replaceAll("[^a-z0-9\\s]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
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
            boolean isDetailQuery = isDetailQuery(userMessage);
            
            if (isDetailQuery) {
                response.append(name).append("\n\n");
            } else {
                response.append("Tôi tìm thấy sản phẩm phù hợp:\n\n");
                response.append(name).append("\n");
            }
            
            // Show brand and category from DB
            if (!brandName.isEmpty() && !brandName.equals("null")) {
                response.append("Thương hiệu: ").append(brandName).append("\n");
            }
            if (!categoryName.isEmpty() && !categoryName.equals("null")) {
                response.append("Danh mục: ").append(categoryName).append("\n");
            }
            if (!sku.isEmpty() && !sku.equals("null")) {
                response.append("Mã sản phẩm: ").append(sku).append("\n");
            }
            
            // Always show description for detail queries, or if available
            String fullDescription = String.valueOf(product.getOrDefault("description", ""));
            if (isDetailQuery && fullDescription.length() > 0 && !fullDescription.equals("null")) {
                // Show more description for detail queries (up to 500 chars)
                String detailDesc = fullDescription.length() > 500 ? 
                    fullDescription.substring(0, 500) + "..." : fullDescription;
                response.append("\nMô tả:\n").append(detailDesc).append("\n\n");
            } else if (description.length() > 0 && !description.equals("null")) {
                response.append("\n").append(description).append("\n");
            }
            
            if (salePrice != null && !salePrice.equals(price)) {
                response.append("\nGiá: ").append(formatPrice(salePrice, categoryName)).append(" VND");
                response.append(" (giảm từ ").append(formatPrice(price, categoryName)).append(" VND)\n");
            } else {
                response.append("\nGiá: ").append(formatPrice(price, categoryName)).append(" VND\n");
            }
            if (rating != null && !rating.equals(0)) {
                response.append("Đánh giá: ").append(rating).append("/5");
                if (reviewCount != null && !reviewCount.equals(0)) {
                    response.append(" (").append(reviewCount).append(" lượt)");
                }
                response.append("\n");
            }
            response.append("Tồn kho: ").append(stock).append(" sản phẩm\n");
            
            if (isDetailQuery) {
                response.append("\nBạn có thể xem thêm hình ảnh và đặt mua ngay trên trang sản phẩm.");
            } else {
                response.append("\nBạn có muốn xem thêm thông tin chi tiết không?");
            }
        } else {
            // Multiple products - check if it's a detail query
            boolean isDetailQuery = isDetailQuery(userMessage);
            
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
                    
                    response.append(name).append("\n");
                    if (!brandName.isEmpty() && !brandName.equals("null")) {
                        response.append("Thương hiệu: ").append(brandName).append("\n");
                    }
                    if (!categoryName.isEmpty() && !categoryName.equals("null")) {
                        response.append("Danh mục: ").append(categoryName).append("\n");
                    }
                    if (!sku.isEmpty() && !sku.equals("null")) {
                        response.append("Mã SP: ").append(sku).append("\n");
                    }
                    if (description.length() > 0 && !description.equals("null")) {
                        response.append(description).append("\n");
                    }
                    if (salePrice != null && !salePrice.equals(price)) {
                        response.append("Giá: ").append(formatPrice(salePrice, categoryName)).append(" VND");
                        response.append(" (giảm từ ").append(formatPrice(price, categoryName)).append(" VND)\n");
                    } else {
                        response.append("Giá: ").append(formatPrice(price, categoryName)).append(" VND\n");
                    }
                    if (rating != null && !rating.equals(0)) {
                        response.append("Đánh giá: ").append(rating).append("/5");
                        if (reviewCount != null && !reviewCount.equals(0)) {
                            response.append(" (").append(reviewCount).append(" lượt)");
                        }
                        response.append("\n");
                    }
                    response.append("Tồn kho: ").append(stock).append(" sản phẩm\n");
                    if (i < products.size() - 1) {
                        response.append("\n\n");
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
                    
                    response.append(count++).append(". ").append(name).append("\n");
                    if (!brandName.isEmpty() && !brandName.equals("null")) {
                        response.append("   ").append(brandName);
                        if (!categoryName.isEmpty() && !categoryName.equals("null")) {
                            response.append(" - ").append(categoryName);
                        }
                        response.append("\n");
                    }
                    if (salePrice != null && !salePrice.equals(price)) {
                        response.append("   Giá: ").append(formatPrice(salePrice, categoryName)).append(" VND");
                        response.append(" (giảm từ ").append(formatPrice(price, categoryName)).append(" VND)\n");
                    } else {
                        response.append("   Giá: ").append(formatPrice(price, categoryName)).append(" VND\n");
                    }
                    if (rating != null && !rating.equals(0)) {
                        response.append("   Đánh giá: ").append(rating).append("/5\n");
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
        String normalizedMessage = normalizeForMatch(message);
        
        // Remove common question words that don't help with search
        String cleanedMessage = lowerMessage
            .replaceAll("\\b(chi tiết|thông tin|mô tả|giới thiệu|về|cho|tôi|bạn|có|không|gì|nào|đó|này|sản phẩm|san pham|sản|phẩm|của|cua|hãng|hang|thương hiệu|thuong hieu)\\b", " ")
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
        // OnePlus: handle "oneplus" and "one plus"
        if (normalizedMessage.contains("oneplus") || normalizedMessage.contains("one plus") || normalizedMessage.contains("1+")) {
            keywords.add("oneplus");
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
                // If the user mentioned OnePlus, don't treat "one"/"plus" as separate keywords (they pull unrelated items).
                if (keywords.contains("oneplus") && (cleanWord.equals("one") || cleanWord.equals("plus"))) {
                    continue;
                }
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
    @SuppressWarnings("unused")
    private String formatPrice(Object price) {
        return formatPrice(price, "");
    }

    private String formatPrice(Object price, String categoryName) {
        if (price == null) return "0";
        try {
            double p = Double.parseDouble(price.toString());
            long vnd = Math.round(p);

            // Heuristic: some datasets store prices in "thousand VND" (e.g. 19990 for 19,990,000 VND).
            // Only upscale when the category typically has high ticket sizes.
            if (vnd > 0 && vnd < 1_000_000 && isHighTicketCategory(categoryName)) {
                vnd = vnd * 1000L;
            }

            // Use locale constant to avoid deprecated Locale(String, String) constructor
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
            nf.setGroupingUsed(true);
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            return nf.format(vnd);
        } catch (Exception e) {
            return price.toString();
        }
    }

    private boolean isHighTicketCategory(String categoryName) {
        if (!StringUtils.hasText(categoryName)) return false;
        String norm = normalizeForMatch(categoryName);
        // English + Vietnamese (normalized, no accents)
        return norm.contains("phone") ||
            norm.contains("smartphone") ||
            norm.contains("dien thoai") ||
            norm.contains("laptop") ||
            norm.contains("macbook") ||
            norm.contains("tablet") ||
            norm.contains("ipad") ||
            norm.contains("headphone") ||
            norm.contains("tai nghe") ||
            norm.contains("watch") ||
            norm.contains("dong ho") ||
            norm.contains("loa") ||
            norm.contains("speaker");
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

        // Avoid false positives like "chi tiet ..." containing "hi"
        if (containsAny(normalized, "xin chào", "chào bạn", "hello") ||
            containsWholeWord(normalized, "hi") ||
            containsWholeWord(normalized, "alo")) {
            // Follow the desired shop greeting template
            return "Xin chào, tôi là AI tư vấn công nghệ. Bạn đang tìm sản phẩm công nghệ nào?";
        }

        // Simple math fallback (when Gemini is temporarily unavailable)
        String mathAnswer = trySolveSimpleMath(normalized);
        if (mathAnswer != null) {
            return mathAnswer;
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

    private boolean containsWholeWord(String text, String word) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(word)) return false;
        return java.util.regex.Pattern
            .compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(text)
            .find();
    }

    private String trySolveSimpleMath(String text) {
        if (text == null) return null;
        // normalize unicode multiply signs etc
        String t = text.replace("×", "*").replace("x", "*");
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\\b(\\d{1,9})\\s*([\\+\\-\\*/])\\s*(\\d{1,9})\\b")
            .matcher(t);
        if (!m.find()) return null;

        try {
            long a = Long.parseLong(m.group(1));
            long b = Long.parseLong(m.group(3));
            String op = m.group(2);
            switch (op) {
                case "+":
                    return String.valueOf(a + b);
                case "-":
                    return String.valueOf(a - b);
                case "*":
                    return String.valueOf(a * b);
                case "/":
                    if (b == 0) return "Không thể chia cho 0.";
                    // If divisible, return integer; otherwise return decimal
                    if (a % b == 0) return String.valueOf(a / b);
                    return String.valueOf(((double) a) / ((double) b));
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
