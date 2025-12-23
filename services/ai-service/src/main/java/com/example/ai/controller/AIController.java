package com.example.ai.controller;

import com.example.ai.entity.AIProvider;
import com.example.ai.entity.AIRequest;
import com.example.ai.entity.AIRequestType;
import com.example.ai.service.AIContentService;
import com.example.ai.service.AIService;
import com.example.ai.service.ChatbotService;
import com.example.ai.service.GeminiService;
import com.example.ai.service.SentimentAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIService aiService;
    
    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private AIContentService aiContentService;
    
    @Autowired
    private GeminiService geminiService;
    
    @Autowired
    private com.example.ai.repository.ChatLogRepository chatLogRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    /**
     * Test Gemini AI connection
     */
    @GetMapping("/test/gemini")
    public ResponseEntity<Map<String, Object>> testGemini() {
        try {
            Map<String, Object> result = aiService.processAIRequest(
                "test-user-1", AIProvider.GEMINI, AIRequestType.CHAT, 
                "Xin chào! Bạn có thể giới thiệu về mình không? Hãy trả lời bằng tiếng Việt.", 
                null, 500
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("provider", "Google Gemini");
            response.put("model", "gemini-2.5-pro");
            response.put("response", result.get("response"));
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("provider", "Google Gemini");
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Test chatbot response
     */
    @PostMapping("/test/chatbot")
    public ResponseEntity<Map<String, Object>> testChatbot(@RequestBody Map<String, String> request) {
        try {
            String message = request.getOrDefault("message", "Tôi muốn mua một chiếc laptop gaming, bạn có thể tư vấn không?");
            
            Map<String, Object> result = chatbotService.processChatMessage("test-user-1", message, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("provider", "Google Gemini");
            response.put("userMessage", message);
            response.put("aiResponse", result.get("response"));
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Test all AI services
     */
    @GetMapping("/test/all-services")
    public ResponseEntity<Map<String, Object>> testAllServices() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Test basic text generation
            Map<String, Object> basicResult = aiService.processAIRequest(
                "test-user-1", AIProvider.GEMINI, AIRequestType.CHAT, 
                "Hello, how are you?", null, 100
            );
            results.put("basicTextGeneration", Map.of(
                "status", "success",
                "response", basicResult.get("response")
            ));
        } catch (Exception e) {
            results.put("basicTextGeneration", Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }

        try {
            // Test chatbot
            Map<String, Object> chatbotResult = chatbotService.processChatMessage(
                "test-user-1", "Tôi cần tư vấn về sản phẩm", null
            );
            results.put("chatbot", Map.of(
                "status", "success",
                "response", chatbotResult.get("response")
            ));
        } catch (Exception e) {
            results.put("chatbot", Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }

        results.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(results);
    }

    /**
     * Generate meeting minutes from topic and agenda
     */
    @PostMapping("/task-suggestion/generate-meeting-minutes")
    public ResponseEntity<Map<String, Object>> generateMeetingMinutes(@RequestBody Map<String, String> request) {
        try {
            String topic = request.get("topic");
            String participants = request.get("participants");
            String agenda = request.get("agenda");

            if (topic == null || participants == null || agenda == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields: topic, participants, agenda"));
            }

            String prompt = String.format(
                "Tạo biên bản cuộc họp với chủ đề: %s\n" +
                "Người tham gia: %s\n" +
                "Chương trình: %s\n\n" +
                "Hãy tạo biên bản cuộc họp chi tiết và chuyên nghiệp.",
                topic, participants, agenda
            );

            Map<String, Object> result = aiService.processAIRequest(
                "test-user-1", AIProvider.GEMINI, AIRequestType.CHAT, 
                prompt, null, 1000
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "meetingMinutes", result.get("response")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Extract tasks from meeting minutes
     */
    @PostMapping("/task-suggestion/extract-tasks")
    public ResponseEntity<Map<String, Object>> extractTasksFromMinutes(@RequestBody Map<String, String> request) {
        try {
            String meetingMinutes = request.get("meetingMinutes");
            if (meetingMinutes == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required field: meetingMinutes"));
            }

            String prompt = String.format(
                "Từ biên bản cuộc họp sau, hãy trích xuất các nhiệm vụ cần thực hiện:\n\n%s\n\n" +
                "Hãy trả về danh sách các nhiệm vụ theo định dạng JSON với các trường: task, assignee, priority, dueDate",
                meetingMinutes
            );

            Map<String, Object> result = aiService.processAIRequest(
                "test-user-1", AIProvider.GEMINI, AIRequestType.CHAT, 
                prompt, null, 1000
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "tasks", result.get("response")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Generate task suggestions based on context
     */
    @PostMapping("/task-suggestion/generate")
    public ResponseEntity<Map<String, Object>> generateTaskSuggestions(@RequestBody Map<String, Object> request) {
        try {
            String context = (String) request.get("context");
            String role = (String) request.get("role");
            String department = (String) request.get("department");

            if (context == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required field: context"));
            }

            String prompt = String.format(
                "Dựa trên bối cảnh: %s\n" +
                "Vai trò: %s\n" +
                "Phòng ban: %s\n\n" +
                "Hãy đề xuất 5-10 nhiệm vụ phù hợp để thực hiện. Trả về theo định dạng JSON.",
                context, role != null ? role : "Nhân viên", department != null ? department : "Tổng hợp"
            );

            Map<String, Object> result = aiService.processAIRequest(
                "test-user-1", AIProvider.GEMINI, AIRequestType.CHAT, 
                prompt, null, 1000
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "suggestions", result.get("response")
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Chat with AI Bot (Main chatbot endpoint) - PUBLIC API
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            // Get userId if provided, otherwise use anonymous
            Object userIdObj = request.get("userId");
            String userId = (userIdObj != null) ? userIdObj.toString() : "anonymous";
            
            String message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Message cannot be empty"));
            }
            
            String sessionId = (String) request.getOrDefault("sessionId", null);
            
            Map<String, Object> result = chatbotService.processChatMessage(userId, message, sessionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in chat endpoint: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Simple chat endpoint - NO AUTH REQUIRED (Public)
     * But accepts userId if user is logged in
     */
    @PostMapping("/chat/public")
    public ResponseEntity<Map<String, Object>> publicChat(@RequestBody Map<String, Object> request) {
        try {
            String message = (String) request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Message cannot be empty"));
            }
            
            // Get userId if provided (user is logged in), otherwise use anonymous
            Object userIdObj = request.get("userId");
            String userId = "anonymous";
            if (userIdObj != null && !userIdObj.toString().trim().isEmpty() && !userIdObj.toString().equals("null")) {
                userId = userIdObj.toString();
                logger.debug("Public chat with logged-in user: {}", userId);
            } else {
                logger.debug("Public chat with anonymous user");
            }
            
            String sessionId = request.get("sessionId") != null ? request.get("sessionId").toString() : null;
            
            Map<String, Object> result = chatbotService.processChatMessage(userId, message, sessionId);
            
            // If circuit breaker is open, provide a fallback response
            if (Boolean.FALSE.equals(result.get("success")) && 
                result.get("error") != null && 
                result.get("error").toString().contains("Circuit breaker")) {
                // Generate a simple fallback response without AI
                Map<String, Object> fallbackResponse = new HashMap<>();
                fallbackResponse.put("success", true);
                fallbackResponse.put("sessionId", result.get("sessionId"));
                fallbackResponse.put("response", "Xin chào! Hiện tại dịch vụ AI đang tạm thời không khả dụng. " +
                    "Bạn có thể tìm kiếm sản phẩm trực tiếp trên trang web hoặc liên hệ bộ phận hỗ trợ. " +
                    "Vui lòng thử lại sau vài phút.");
                fallbackResponse.put("fallback", true);
                return ResponseEntity.ok(fallbackResponse);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in public chat: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Reset circuit breaker
     */
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker() {
        try {
            geminiService.resetCircuitBreaker();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Circuit breaker reset successfully"
            ));
        } catch (Exception e) {
            logger.error("Error resetting circuit breaker: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Get circuit breaker status
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        try {
            Map<String, Object> status = geminiService.getCircuitBreakerStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting circuit breaker status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get chat history for a user
     */
    @GetMapping("/chat/history/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(@PathVariable String userId) {
        try {
            List<Map<String, Object>> history = chatbotService.getChatHistory(userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get chat sessions for a user
     */
    @GetMapping("/chat/sessions/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getChatSessions(@PathVariable String userId) {
        try {
            List<Map<String, Object>> sessions = chatbotService.getChatSessions(userId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create new chat session
     */
    @PostMapping("/chat/session")
    public ResponseEntity<Map<String, Object>> createChatSession(@RequestBody Map<String, Object> request) {
        try {
            String userId = request.get("userId").toString();
            String title = (String) request.getOrDefault("title", "New Chat");
            
            Map<String, Object> result = chatbotService.createChatSession(userId, title);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * End chat session
     */
    @PostMapping("/chat/session/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endChatSession(@PathVariable String sessionId) {
        try {
            Map<String, Object> result = chatbotService.endChatSession(sessionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get product recommendations based on chat context
     */
    @PostMapping("/chat/product-recommendations")
    public ResponseEntity<Map<String, Object>> getChatProductRecommendations(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            String query = request.get("query");
            
            if (sessionId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "sessionId is required"));
            }
            
            List<Map<String, Object>> products = chatbotService.getChatProductRecommendations(sessionId, query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());
            response.put("sessionId", sessionId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get AI request by ID
     */
    @GetMapping("/requests/{id}")
    public ResponseEntity<Map<String, Object>> getRequest(@PathVariable Long id) {
        try {
            Optional<AIRequest> requestOpt = aiService.findById(id);
            if (requestOpt.isPresent()) {
                return ResponseEntity.ok(createRequestResponse(requestOpt.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get AI requests by user ID
     */
    @GetMapping("/requests/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserRequests(@PathVariable String userId) {
        try {
            List<AIRequest> requests = aiService.findByUserId(userId);
            List<Map<String, Object>> responses = requests.stream()
                .map(this::createRequestResponse)
                .toList();
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all AI requests with pagination
     */
    @GetMapping("/requests")
    public ResponseEntity<Page<Map<String, Object>>> getAllRequests(Pageable pageable) {
        try {
            Page<AIRequest> requests = aiService.findAll(pageable);
            Page<Map<String, Object>> responses = requests.map(this::createRequestResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get AI request statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAIRequestStatistics() {
        try {
            Map<String, Object> stats = aiService.getAIRequestStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // SENTIMENT ANALYSIS ENDPOINTS
    // ==========================================

    /**
     * Analyze sentiment of text (review, comment, feedback)
     */
    @PostMapping("/sentiment/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
            }

            SentimentAnalysisService.SentimentResult result = sentimentAnalysisService.analyzeSentiment(text);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sentiment", result.getSentiment());
            response.put("confidence", result.getConfidence());
            response.put("explanation", result.getExplanation());
            response.put("category", result.getCategory());
            response.put("isNegative", sentimentAnalysisService.isNegativeSentiment(result));
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Analyze sentiment for multiple texts (batch)
     */
    @PostMapping("/sentiment/analyze-batch")
    public ResponseEntity<Map<String, Object>> analyzeSentimentBatch(@RequestBody Map<String, String> texts) {
        try {
            if (texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one text is required"));
            }

            Map<String, SentimentAnalysisService.SentimentResult> results = sentimentAnalysisService.analyzeSentimentBatch(texts);
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> processedResults = new HashMap<>();
            
            results.forEach((key, result) -> {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("sentiment", result.getSentiment());
                resultMap.put("confidence", result.getConfidence());
                resultMap.put("explanation", result.getExplanation());
                resultMap.put("category", result.getCategory());
                resultMap.put("isNegative", sentimentAnalysisService.isNegativeSentiment(result));
                processedResults.put(key, resultMap);
            });
            
            response.put("success", true);
            response.put("results", processedResults);
            response.put("count", results.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==========================================
    // AI CONTENT GENERATION ENDPOINTS
    // ==========================================

    /**
     * Generate product description
     */
    @PostMapping("/content/product-description")
    public ResponseEntity<Map<String, Object>> generateProductDescription(@RequestBody Map<String, Object> request) {
        try {
            String productName = (String) request.get("productName");
            String category = (String) request.get("category");
            String brand = (String) request.get("brand");
            @SuppressWarnings("unchecked")
            List<String> features = (List<String>) request.get("features");
            @SuppressWarnings("unchecked")
            List<String> specifications = (List<String>) request.get("specifications");

            if (productName == null || category == null || brand == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productName, category, and brand are required"));
            }

            String description = aiContentService.generateProductDescription(productName, category, brand, features, specifications);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("description", description);
            response.put("productName", productName);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Generate marketing copy
     */
    @PostMapping("/content/marketing-copy")
    public ResponseEntity<Map<String, Object>> generateMarketingCopy(@RequestBody Map<String, String> request) {
        try {
            String productName = request.get("productName");
            String targetAudience = request.get("targetAudience");
            String tone = request.get("tone");

            if (productName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productName is required"));
            }

            String marketingCopy = aiContentService.generateMarketingCopy(productName, targetAudience, tone);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("marketingCopy", marketingCopy);
            response.put("productName", productName);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Generate SEO description
     */
    @PostMapping("/content/seo-description")
    public ResponseEntity<Map<String, Object>> generateSEODescription(@RequestBody Map<String, String> request) {
        try {
            String productName = request.get("productName");
            String category = request.get("category");
            String mainKeyword = request.get("mainKeyword");

            if (productName == null || category == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productName and category are required"));
            }

            String seoDescription = aiContentService.generateSEODescription(productName, category, mainKeyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("seoDescription", seoDescription);
            response.put("length", seoDescription.length());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Suggest product tags
     */
    @PostMapping("/content/suggest-tags")
    public ResponseEntity<Map<String, Object>> suggestProductTags(@RequestBody Map<String, String> request) {
        try {
            String productName = request.get("productName");
            String category = request.get("category");
            String description = request.get("description");

            if (productName == null || category == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productName and category are required"));
            }

            List<String> tags = aiContentService.suggestProductTags(productName, category, description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tags", tags);
            response.put("count", tags.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Generate title variations for A/B testing
     */
    @PostMapping("/content/title-variations")
    public ResponseEntity<Map<String, Object>> generateTitleVariations(@RequestBody Map<String, Object> request) {
        try {
            String productName = (String) request.get("productName");
            Integer count = request.get("count") != null ? 
                           Integer.valueOf(request.get("count").toString()) : 5;

            if (productName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productName is required"));
            }

            List<String> variations = aiContentService.generateTitleVariations(productName, count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalTitle", productName);
            response.put("variations", variations);
            response.put("count", variations.size());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Create AI request response DTO
     */
    private Map<String, Object> createRequestResponse(AIRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", request.getId());
        response.put("userId", request.getUserId());
        response.put("aiProvider", request.getAiProvider().name());
        response.put("requestType", request.getRequestType().name());
        response.put("prompt", request.getPrompt());
        response.put("response", request.getResponse());
        response.put("status", request.getStatus());
        response.put("tokensUsed", request.getTokensUsed());
        response.put("cost", request.getCost());
        response.put("processingTimeMs", request.getProcessingTimeMs());
        response.put("errorMessage", request.getErrorMessage());
        response.put("createdAt", request.getCreatedAt());
        response.put("updatedAt", request.getUpdatedAt());
        
        return response;
    }

    /**
     * Generate text using Gemini AI
     */
    @PostMapping("/gemini/generate-text")
    public ResponseEntity<Map<String, Object>> generateText(@RequestBody Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) request.get("variables");
            
            if (prompt == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "prompt is required"));
            }

            String result = geminiService.generateText(prompt, variables != null ? variables : new HashMap<>());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate embedding using Gemini AI
     */
    @PostMapping("/gemini/generate-embedding")
    public ResponseEntity<Map<String, Object>> generateEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            
            if (text == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
            }

            List<Double> embedding = geminiService.generateEmbedding(text);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("embedding", embedding);
            response.put("dimension", embedding.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate product description using AI
     */
    @PostMapping("/gemini/product-description")
    public ResponseEntity<Map<String, Object>> generateProductDescriptionGemini(@RequestBody Map<String, Object> request) {
        try {
            String productName = (String) request.get("productName");
            String category = (String) request.get("category");
            String brand = (String) request.get("brand");
            @SuppressWarnings("unchecked")
            List<String> features = (List<String>) request.get("features");
            @SuppressWarnings("unchecked")
            List<String> specifications = (List<String>) request.get("specifications");
            
            if (productName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "productName is required"));
            }

            String description = geminiService.generateProductDescription(
                productName, 
                category != null ? category : "", 
                brand != null ? brand : "",
                features != null ? features : List.of(),
                specifications != null ? specifications : List.of()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("description", description);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Analyze sentiment using Gemini AI
     */
    @PostMapping("/gemini/sentiment")
    public ResponseEntity<Map<String, Object>> analyzeSentimentGemini(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            
            if (text == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
            }

            String sentiment = geminiService.analyzeSentiment(text);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sentiment", sentiment);
            response.put("text", text);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate chatbot response using Gemini AI
     */
    @PostMapping("/gemini/chatbot")
    public ResponseEntity<Map<String, Object>> generateChatbotResponse(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            String context = request.get("context");
            
            if (message == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
            }

            String response = geminiService.generateChatbotResponse(message, context != null ? context : "");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("response", response);
            result.put("message", message);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== ADMIN ENDPOINTS FOR CHAT LOGS ====================
    
    /**
     * Get all chat logs with pagination (Admin)
     */
    @GetMapping("/admin/chat-logs")
    public ResponseEntity<Map<String, Object>> getAllChatLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Boolean isProductRelated) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<com.example.ai.entity.ChatLog> chatLogs;
            
            if (userId != null && !userId.isEmpty()) {
                chatLogs = chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            } else if (isProductRelated != null) {
                if (isProductRelated) {
                    chatLogs = chatLogRepository.findByIsProductRelatedTrueOrderByCreatedAtDesc(pageable);
                } else {
                    chatLogs = chatLogRepository.findAll(pageable);
                }
            } else {
                chatLogs = chatLogRepository.findAll(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", chatLogs.getContent());
            response.put("totalElements", chatLogs.getTotalElements());
            response.put("totalPages", chatLogs.getTotalPages());
            response.put("currentPage", chatLogs.getNumber());
            response.put("size", chatLogs.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting chat logs: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Get chat log statistics (Admin)
     */
    @GetMapping("/admin/chat-logs/statistics")
    public ResponseEntity<Map<String, Object>> getChatLogStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            stats.put("totalLogs", chatLogRepository.count());
            stats.put("productQueriesWithResults", chatLogRepository.countProductQueriesWithResults());
            stats.put("productQueriesWithoutResults", chatLogRepository.countProductQueriesWithoutResults());
            stats.put("aiQueries", chatLogRepository.countAIQueries());
            
            // Response source breakdown
            List<Object[]> sourceBreakdown = chatLogRepository.countByResponseSource();
            Map<String, Long> sourceStats = new HashMap<>();
            for (Object[] row : sourceBreakdown) {
                sourceStats.put(String.valueOf(row[0]), (Long) row[1]);
            }
            stats.put("responseSourceBreakdown", sourceStats);
            
            // Average rating
            Double avgRating = chatLogRepository.getAverageRating();
            stats.put("averageRating", avgRating != null ? avgRating : 0.0);
            
            // Most asked product queries (that didn't find results)
            Pageable top10 = PageRequest.of(0, 10);
            List<Object[]> topQueries = chatLogRepository.findMostAskedProductQueries(top10);
            List<Map<String, Object>> topQueriesList = new ArrayList<>();
            for (Object[] row : topQueries) {
                Map<String, Object> query = new HashMap<>();
                query.put("query", row[0]);
                query.put("count", row[1]);
                topQueriesList.add(query);
            }
            stats.put("mostAskedProductQueries", topQueriesList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting chat log statistics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Submit feedback for a chat log
     */
    @PostMapping("/chat-logs/{logId}/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable Long logId,
            @RequestBody Map<String, Object> request) {
        try {
            com.example.ai.entity.ChatLog chatLog = chatLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Chat log not found"));
            
            String feedback = (String) request.get("feedback");
            Integer rating = request.get("rating") != null ? 
                Integer.valueOf(request.get("rating").toString()) : null;
            
            if (feedback != null && !feedback.isEmpty()) {
                chatLog.setFeedback(feedback);
            }
            if (rating != null && rating >= 1 && rating <= 5) {
                chatLog.setRating(rating);
            }
            
            chatLogRepository.save(chatLog);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Feedback submitted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error submitting feedback: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Get chat logs for a specific user (Admin)
     */
    @GetMapping("/admin/chat-logs/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserChatLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<com.example.ai.entity.ChatLog> chatLogs = chatLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", chatLogs.getContent());
            response.put("totalElements", chatLogs.getTotalElements());
            response.put("totalPages", chatLogs.getTotalPages());
            response.put("currentPage", chatLogs.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user chat logs: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Get product queries that didn't find results (for analysis)
     */
    @GetMapping("/admin/chat-logs/missing-products")
    public ResponseEntity<Map<String, Object>> getMissingProductQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<com.example.ai.entity.ChatLog> chatLogs = 
                chatLogRepository.findByIsProductRelatedTrueAndFoundProductsFalseOrderByCreatedAtDesc(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", chatLogs.getContent());
            response.put("totalElements", chatLogs.getTotalElements());
            response.put("totalPages", chatLogs.getTotalPages());
            response.put("currentPage", chatLogs.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting missing product queries: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}