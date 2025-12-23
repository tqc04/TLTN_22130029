package com.example.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    
    @Value("${gemini.api.key:}")
    private String apiKey;
    
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent}")
    private String apiUrl;
    
    @Value("${gemini.rate.limit.rps:1}")
    private int maxRequestsPerSecond;
    
    @Value("${gemini.retry.max-attempts:2}")
    private int retryMaxAttempts;
    
    @Value("${gemini.retry.base-backoff-ms:500}")
    private long retryBaseBackoffMs;
    
    @Value("${gemini.embedding.model:text-embedding-004}")
    private String embeddingModel;
    
    @Value("${gemini.embedding.url:https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent}")
    private String embeddingUrl;
    
    private final WebClient webClient;
    
    // Simple client-side rate limiter: N requests per second
    private final Deque<Instant> recentRequests = new ArrayDeque<>();
    
    // Circuit breaker state
    private volatile boolean circuitOpen = false;
    private volatile Instant circuitOpenTime;
    private static final Duration CIRCUIT_TIMEOUT = Duration.ofMinutes(2); // Reduced from 5 to 2 minutes
    private volatile int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 5; // Reduced from 10 to 5

    public GeminiService() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(120))
            .followRedirect(true);
            
        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Generate text using Gemini API
     */
    public String generateText(String prompt) {
        return generateText(prompt, new HashMap<>());
    }

    /**
     * Generate text using Gemini API with variables
     */
    public String generateText(String prompt, Map<String, Object> variables) {
        if (circuitOpen) {
            if (Duration.between(circuitOpenTime, Instant.now()).compareTo(CIRCUIT_TIMEOUT) > 0) {
        circuitOpen = false;
                logger.info("Circuit breaker reset - attempting request");
            } else {
                throw new RuntimeException("Circuit breaker is open - service temporarily unavailable");
            }
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        // Rate limiting
        enforceRateLimit();

        // Replace variables in prompt
        String processedPrompt = prompt;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processedPrompt = processedPrompt.replace(placeholder, value);
        }

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", processedPrompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));
        
        // Generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 2048);
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        try {
            String url = apiUrl + "?key=" + apiKey;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                            .retrieve()
                .bodyToMono(Map.class)
                            .block();

            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = (Map<String, Object>) candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content2 = candidate != null ? (Map<String, Object>) candidate.get("content") : null;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = content2 != null ? (List<Map<String, Object>>) content2.get("parts") : null;
                    if (parts != null && !parts.isEmpty()) {
                        Object t = parts.get(0).get("text");
                        if (t != null) return String.valueOf(t);
                    }
                    // Fallback: try "promptFeedback" or finish reason for better error
                    Object finishReason = candidate != null ? candidate.get("finishReason") : null;
                    throw new RuntimeException("Model returned no content" + (finishReason != null ? (" - finishReason=" + finishReason) : ""));
                }
            }
            
            throw new RuntimeException("No valid response from Gemini API");
            
        } catch (WebClientResponseException e) {
            logger.error("Gemini API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            if (e.getStatusCode().is5xxServerError()) {
                circuitOpen = true;
                circuitOpenTime = Instant.now();
                throw new RuntimeException("Gemini API server error - circuit breaker opened");
            }
            
            String msg = e.getResponseBodyAsString();
            if (msg == null || msg.isBlank()) msg = e.toString();
            throw new RuntimeException("Gemini API error: " + msg);
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) msg = e.toString();
            throw new RuntimeException("Error calling Gemini API: " + msg);
        }
    }

    /**
     * Generate text with optional system message and max tokens, returning a rich result map
     * to be compatible with existing service expectations.
     */
    public Map<String, Object> generateText(String prompt, String systemMessage, int maxTokens) {
        String combinedPrompt;
        if (systemMessage != null && !systemMessage.trim().isEmpty()) {
            combinedPrompt = systemMessage.trim() + "\n\n" + prompt;
        } else {
            combinedPrompt = prompt;
        }

        // Build variables empty for now; reuse existing pipeline
        if (circuitOpen) {
            if (Duration.between(circuitOpenTime, Instant.now()).compareTo(CIRCUIT_TIMEOUT) > 0) {
                circuitOpen = false;
                consecutiveFailures = 0;
                logger.info("Circuit breaker reset - attempting request");
            } else {
                return Map.of(
                    "success", false,
                    "error", "Circuit breaker is open - service temporarily unavailable",
                    "circuitBreakerOpen", true,
                    "retryAfterSeconds", CIRCUIT_TIMEOUT.getSeconds() - Duration.between(circuitOpenTime, Instant.now()).getSeconds()
                );
            }
        }

        if (apiKey == null || apiKey.isEmpty()) {
            return Map.of("success", false, "error", "Gemini API key not configured");
        }

        enforceRateLimit();

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", combinedPrompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", Math.max(1, maxTokens));
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        try {
            String url = apiUrl + "?key=" + apiKey;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            String text = null;
            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = (Map<String, Object>) candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content2 = (Map<String, Object>) candidate.get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = content2 != null ? (List<Map<String, Object>>) content2.get("parts") : null;
                    if (parts != null && !parts.isEmpty()) {
                        Object t = parts.get(0).get("text");
                        if (t != null) text = String.valueOf(t);
                    }
                    if (text == null) {
                        Object finishReason = candidate.get("finishReason");
                        Object safety = candidate.get("safetyRatings");
                        return Map.of(
                            "success", false,
                            "error", "No valid content from Gemini API" + (finishReason != null ? (" - finishReason=" + finishReason) : ""),
                            "details", safety
                        );
                    }
                }
            }

            if (text == null) {
                consecutiveFailures++;
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    circuitOpen = true;
                    circuitOpenTime = Instant.now();
                }
                return Map.of("success", false, "error", "No valid response from Gemini API");
            }

            // Reset failure count on success
            consecutiveFailures = 0;
            
            int tokensUsed = Math.min(Math.max(text.length() / 4, 50), Math.max(50, maxTokens));
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("text", text);
            result.put("tokensUsed", tokensUsed);
            return result;
        } catch (WebClientResponseException e) {
            logger.error("Gemini API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            consecutiveFailures++;
            if (consecutiveFailures >= FAILURE_THRESHOLD || e.getStatusCode().is5xxServerError()) {
                circuitOpen = true;
                circuitOpenTime = Instant.now();
                logger.warn("Circuit breaker opened after {} failures", consecutiveFailures);
                return Map.of("success", false, "error", "Gemini API server error - circuit breaker opened");
            }
            
            return Map.of("success", false, "error", "Gemini API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            consecutiveFailures++;
            if (consecutiveFailures >= FAILURE_THRESHOLD) {
                circuitOpen = true;
                circuitOpenTime = Instant.now();
                logger.warn("Circuit breaker opened after {} failures", consecutiveFailures);
            }
            return Map.of("success", false, "error", "Error calling Gemini API: " + e.getMessage());
        }
    }

    /**
     * Generate embedding for text
     */
    public List<Double> generateEmbedding(String text) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", text)));
        requestBody.put("content", content);

        try {
            String url = embeddingUrl + "?key=" + apiKey;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("embedding")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
                @SuppressWarnings("unchecked")
                List<Double> values = (List<Double>) embedding.get("values");
                return values;
            }
            
            throw new RuntimeException("No valid embedding from Gemini API");
            
        } catch (Exception e) {
            logger.error("Error generating embedding", e);
            throw new RuntimeException("Error generating embedding: " + e.getMessage());
        }
    }

    /**
     * Generate product description using AI
     */
    public String generateProductDescription(String productName, String category, String brand, 
                                           List<String> features, List<String> specifications) {
        String prompt = String.format(
            "Tạo mô tả sản phẩm chuyên nghiệp cho %s %s %s. " +
            "Tính năng: %s. Thông số kỹ thuật: %s. " +
            "Mô tả phải hấp dẫn, chuyên nghiệp và dài khoảng 200-300 từ.",
            brand, productName, category,
            String.join(", ", features),
            String.join(", ", specifications)
        );
        
        return generateText(prompt);
    }

    /**
     * Analyze sentiment of text
     */
    public String analyzeSentiment(String text) {
        String prompt = String.format(
            "Phân tích cảm xúc của văn bản sau và trả về một trong các giá trị: POSITIVE, NEGATIVE, NEUTRAL. " +
            "Văn bản: %s",
            text
        );
        
        return generateText(prompt);
    }

    /**
     * Generate chatbot response
     */
    public String generateChatbotResponse(String userMessage, String context) {
        String prompt = String.format(
            "Bạn là trợ lý bán hàng thông minh của cửa hàng điện tử. " +
            "Trả lời câu hỏi của khách hàng một cách thân thiện và hữu ích. " +
            "Context: %s. " +
            "Câu hỏi: %s",
            context, userMessage
        );
        
        return generateText(prompt);
    }

    /**
     * Reset circuit breaker manually
     */
    public void resetCircuitBreaker() {
        circuitOpen = false;
        consecutiveFailures = 0;
        circuitOpenTime = null;
        logger.info("Circuit breaker manually reset");
    }
    
    /**
     * Get circuit breaker status
     */
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("circuitOpen", circuitOpen);
        status.put("consecutiveFailures", consecutiveFailures);
        if (circuitOpen && circuitOpenTime != null) {
            long secondsRemaining = CIRCUIT_TIMEOUT.getSeconds() - Duration.between(circuitOpenTime, Instant.now()).getSeconds();
            status.put("retryAfterSeconds", Math.max(0, secondsRemaining));
            status.put("openedAt", circuitOpenTime.toString());
        }
        return status;
    }

    /**
     * Enforce rate limiting
     */
    private void enforceRateLimit() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofSeconds(1));
        
        // Remove old requests
        while (!recentRequests.isEmpty() && recentRequests.peekFirst().isBefore(cutoff)) {
            recentRequests.removeFirst();
        }
        
        // Check if we're at the limit
        if (recentRequests.size() >= maxRequestsPerSecond) {
            try {
                Thread.sleep(1000); // Wait 1 second
                enforceRateLimit(); // Recursive call
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiting interrupted", e);
            }
        }
        
        recentRequests.addLast(now);
    }
}