package com.example.ai.service;

import com.example.ai.entity.AIRequest;
import com.example.ai.entity.AIProvider;
import com.example.ai.entity.AIRequestType;
import com.example.ai.repository.AIRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@Transactional
public class AIService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    
    @Autowired
    private AIRequestRepository aiRequestRepository;
    
    
    @Autowired
    private GeminiService geminiService;

    /**
     * Process AI request
     */
    public Map<String, Object> processAIRequest(String userId, AIProvider provider, AIRequestType requestType, 
                                               String prompt, String systemMessage, Integer maxTokens) {
        try {
            // Create AI request record
            AIRequest aiRequest = new AIRequest(userId, provider, requestType, prompt);
            aiRequest.setStatus("PROCESSING");
            aiRequest = aiRequestRepository.save(aiRequest);
            
            long startTime = System.currentTimeMillis();
            
            // Process request based on provider
            Map<String, Object> result = processWithProvider(provider, prompt, systemMessage, maxTokens, requestType);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Update request with results
            if (Boolean.TRUE.equals(result.get("success"))) {
                aiRequest.setStatus("COMPLETED");
                // Handle both "text" and "response" keys
                String responseText = (String) result.getOrDefault("text", result.get("response"));
                if (responseText == null) {
                    responseText = result.get("text") != null ? String.valueOf(result.get("text")) : null;
                }
                aiRequest.setResponse(responseText);
                Object tokensUsedObj = result.get("tokensUsed");
                if (tokensUsedObj != null) {
                    aiRequest.setTokensUsed((Integer) tokensUsedObj);
                    aiRequest.setCost(calculateCost(provider, (Integer) tokensUsedObj));
                }
            } else {
                aiRequest.setStatus("FAILED");
                aiRequest.setErrorMessage((String) result.get("error"));
            }
            
            aiRequest.setProcessingTimeMs(processingTime);
            aiRequestRepository.save(aiRequest);
            
            // Create a mutable copy to add request ID and normalize response key
            Map<String, Object> response = new HashMap<>(result);
            response.put("requestId", aiRequest.getId());
            response.put("processingTimeMs", processingTime);
            
            // Normalize response text
            if (response.containsKey("text") && !response.containsKey("response")) {
                response.put("response", response.get("text"));
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error processing AI request: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Process request with specific provider
     */
    private Map<String, Object> processWithProvider(AIProvider provider, String prompt, String systemMessage, 
                                                   Integer maxTokens, AIRequestType requestType) {
        switch (provider) {
            case GEMINI:
                return processWithGemini(prompt, systemMessage, maxTokens, requestType);
            default:
                return Map.of("success", false, "error", "Unsupported AI provider: " + provider);
        }
    }


    /**
     * Process with Gemini
     */
    private Map<String, Object> processWithGemini(String prompt, String systemMessage, Integer maxTokens, AIRequestType requestType) {
        // Build context-specific system message based on request type
        String contextualSystemMessage = buildSystemMessage(systemMessage, requestType);
        
        // Use the available generateText method
        return geminiService.generateText(prompt, contextualSystemMessage, maxTokens);
    }
    
    /**
     * Build context-specific system message based on request type
     */
    private String buildSystemMessage(String baseSystemMessage, AIRequestType requestType) {
        StringBuilder systemMessage = new StringBuilder();
        
        if (baseSystemMessage != null && !baseSystemMessage.trim().isEmpty()) {
            systemMessage.append(baseSystemMessage).append("\n\n");
        }
        
        switch (requestType) {
            case PRODUCT_DESCRIPTION:
                systemMessage.append("You are an expert product description writer. Generate compelling, SEO-friendly product descriptions that highlight key features and benefits.");
                break;
            case PRODUCT_RECOMMENDATION:
                systemMessage.append("You are a product recommendation expert. Analyze user preferences and suggest relevant products with detailed explanations.");
                break;
            case SEARCH_QUERY:
                systemMessage.append("You are a search optimization expert. Generate relevant search suggestions and queries based on user input.");
                break;
            case TRANSLATION:
                systemMessage.append("You are a professional translator. Provide accurate, natural translations while preserving the original meaning and tone.");
                break;
            case SUMMARIZATION:
                systemMessage.append("You are a summarization expert. Create concise, accurate summaries that capture the key points and main ideas.");
                break;
            case SENTIMENT_ANALYSIS:
                systemMessage.append("You are a sentiment analysis expert. Analyze the emotional tone and sentiment of the given text.");
                break;
            case CHAT:
                systemMessage.append("You are a helpful AI assistant. Provide friendly, accurate, and helpful responses to user questions.");
                break;
            default:
                systemMessage.append("You are a helpful AI assistant. Provide accurate and useful responses to user requests.");
                break;
        }
        
        return systemMessage.toString();
    }

    /**
     * Calculate cost based on provider and tokens
     */
    private Double calculateCost(AIProvider provider, Integer tokens) {
        if (tokens == null) return 0.0;
        
        switch (provider) {
            case GEMINI:
                return tokens * 0.00001; // $0.01 per 1K tokens
            default:
                return 0.0;
        }
    }

    /**
     * Get AI request by ID
     */
    public Optional<AIRequest> findById(Long id) {
        return aiRequestRepository.findById(id);
    }

    /**
     * Get AI requests by user ID
     */
    public List<AIRequest> findByUserId(String userId) {
        return aiRequestRepository.findByUserId(userId);
    }

    /**
     * Get AI requests by provider
     */
    public List<AIRequest> findByProvider(AIProvider provider) {
        return aiRequestRepository.findByAiProvider(provider);
    }

    /**
     * Get AI requests by type
     */
    public List<AIRequest> findByRequestType(AIRequestType requestType) {
        return aiRequestRepository.findByRequestType(requestType);
    }

    /**
     * Get all AI requests with pagination
     */
    public Page<AIRequest> findAll(Pageable pageable) {
        return aiRequestRepository.findAll(pageable);
    }

    /**
     * Get AI request statistics
     */
    public Map<String, Object> getAIRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRequests", aiRequestRepository.count());
        stats.put("completedRequests", aiRequestRepository.countByUserIdAndStatus(null, "COMPLETED"));
        stats.put("failedRequests", aiRequestRepository.countByUserIdAndStatus(null, "FAILED"));
        stats.put("pendingRequests", aiRequestRepository.countByUserIdAndStatus(null, "PENDING"));
        
        return stats;
    }

    /**
     * Get user AI usage statistics
     */
    public Map<String, Object> getUserAIUsageStatistics(String userId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRequests", aiRequestRepository.countByUserIdAndStatus(userId, "COMPLETED"));
        stats.put("totalTokens", aiRequestRepository.sumTokensUsedByUserId(userId));
        stats.put("totalCost", aiRequestRepository.sumCostByUserId(userId));
        
        return stats;
    }

    /**
     * Get pending requests
     */
    public List<AIRequest> getPendingRequests() {
        return aiRequestRepository.findPendingRequests();
    }

    /**
     * Get failed requests
     */
    public List<AIRequest> getFailedRequests() {
        return aiRequestRepository.findFailedRequests(LocalDateTime.now().minusDays(7));
    }

    /**
     * Retry failed request
     */
    public Map<String, Object> retryRequest(Long requestId) {
        try {
            Optional<AIRequest> requestOpt = aiRequestRepository.findById(requestId);
            if (requestOpt.isEmpty()) {
                return Map.of("success", false, "error", "Request not found");
            }
            
            AIRequest request = requestOpt.get();
            if (!"FAILED".equals(request.getStatus())) {
                return Map.of("success", false, "error", "Request is not in failed status");
            }
            
            // Reset request for retry
            request.setStatus("PENDING");
            request.setErrorMessage(null);
            aiRequestRepository.save(request);
            
            // Process request again
            Map<String, Object> result = processAIRequest(
                request.getUserId(),    
                request.getAiProvider(),
                request.getRequestType(),
                request.getPrompt(),
                null,
                1000
            );
            
            return result;
        } catch (Exception e) {
            logger.error("Error retrying request: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Delete AI request
     */
    public void deleteRequest(Long requestId) {
        aiRequestRepository.deleteById(requestId);
    }

    /**
     * Get AI provider status
     */
    public Map<String, Object> getProviderStatus(AIProvider provider) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test provider with simple request
            Map<String, Object> testResult = processWithProvider(provider, "Hello", "You are a helpful assistant.", 10, AIRequestType.CHAT);
            
            status.put("provider", provider.name());
            status.put("status", (Boolean) testResult.get("success") ? "ONLINE" : "OFFLINE");
            status.put("lastChecked", LocalDateTime.now());
            status.put("error", testResult.get("error"));
        } catch (Exception e) {
            status.put("provider", provider.name());
            status.put("status", "OFFLINE");
            status.put("lastChecked", LocalDateTime.now());
            status.put("error", e.getMessage());
        }
        
        return status;
    }

    /**
     * Get all provider statuses
     */
    public Map<String, Object> getAllProviderStatuses() {
        Map<String, Object> statuses = new HashMap<>();
        
        for (AIProvider provider : AIProvider.values()) {
            statuses.put(provider.name(), getProviderStatus(provider));
        }
        
        return statuses;
    }
}