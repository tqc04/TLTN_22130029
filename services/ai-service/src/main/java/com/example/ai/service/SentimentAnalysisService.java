package com.example.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SentimentAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalysisService.class);
    
    // Sentiment constants
    private static final String SENTIMENT_POSITIVE = "POSITIVE";
    private static final String SENTIMENT_NEGATIVE = "NEGATIVE";
    private static final String SENTIMENT_NEUTRAL = "NEUTRAL";
    
    // Thresholds
    private static final double DEFAULT_CONFIDENCE = 0.5;
    private static final double NEGATIVE_FLAG_THRESHOLD = 0.7;
    private static final double FALLBACK_CONFIDENCE = 0.8;
    
    // AI Configuration
    private static final int MAX_TOKENS = 500;
    private static final String SYSTEM_MESSAGE = "You are a sentiment analysis expert. Always return valid JSON format exactly as specified.";
    
    @Autowired
    private GeminiService geminiService;

    /**
     * Sentiment Analysis Result
     */
    public static class SentimentResult {
        private String sentiment;      // POSITIVE, NEGATIVE, NEUTRAL
        private double confidence;      // 0.0 to 1.0
        private String explanation;
        private String category;        // e.g., "Product Quality", "Customer Service"
        
        public SentimentResult(String sentiment, double confidence, String explanation) {
            this.sentiment = sentiment;
            this.confidence = confidence;
            this.explanation = explanation;
        }

        public String getSentiment() {
            return sentiment;
        }

        public void setSentiment(String sentiment) {
            this.sentiment = sentiment;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }

    /**
     * Analyze sentiment of a text (review, comment, feedback)
     */
    public SentimentResult analyzeSentiment(String text) {
        try {
            String prompt = String.format(
                "Phân tích sentiment của văn bản sau và trả về kết quả theo định dạng JSON chính xác:\n\n" +
                "Văn bản: \"%s\"\n\n" +
                "Trả về JSON với format:\n" +
                "{\n" +
                "  \"sentiment\": \"POSITIVE hoặc NEGATIVE hoặc NEUTRAL\",\n" +
                "  \"confidence\": số từ 0.0 đến 1.0,\n" +
                "  \"explanation\": \"giải thích ngắn gọn bằng tiếng Việt\",\n" +
                "  \"category\": \"phân loại chủ đề (ví dụ: Chất lượng sản phẩm, Dịch vụ khách hàng)\"\n" +
                "}\n\n" +
                "CHỈ trả về JSON, không có text khác.",
                text
            );

            Map<String, Object> result = geminiService.generateText(prompt, SYSTEM_MESSAGE, MAX_TOKENS);
            
            if ((Boolean) result.get("success")) {
                String responseText = (String) result.get("text");
                return parseSentimentResponse(responseText, text);
            } else {
                logger.error("Failed to analyze sentiment: {}", result.get("error"));
                return new SentimentResult(SENTIMENT_NEUTRAL, DEFAULT_CONFIDENCE, "AI service không khả dụng");
            }
            
        } catch (Exception e) {
            logger.error("Error analyzing sentiment: {}", e.getMessage(), e);
            return new SentimentResult(SENTIMENT_NEUTRAL, DEFAULT_CONFIDENCE, "Lỗi khi phân tích: " + e.getMessage());
        }
    }

    /**
     * Analyze sentiment for multiple texts (batch)
     */
    public Map<String, SentimentResult> analyzeSentimentBatch(Map<String, String> texts) {
        Map<String, SentimentResult> results = new HashMap<>();
        
        for (Map.Entry<String, String> entry : texts.entrySet()) {
            results.put(entry.getKey(), analyzeSentiment(entry.getValue()));
        }
        
        return results;
    }

    /**
     * Check if sentiment is negative (for auto-flagging)
     * 
     * @param result Sentiment analysis result
     * @return true if sentiment is negative with high confidence
     */
    public boolean isNegativeSentiment(SentimentResult result) {
        return SENTIMENT_NEGATIVE.equals(result.getSentiment()) && 
               result.getConfidence() >= NEGATIVE_FLAG_THRESHOLD;
    }

    /**
     * Parse sentiment response from AI
     */
    private SentimentResult parseSentimentResponse(String responseText, String originalText) {
        try {
            // Try to extract JSON from response
            String jsonText = extractJSON(responseText);
            
            if (jsonText != null) {
                // Parse JSON manually (simple parsing)
                String sentiment = extractValue(jsonText, "sentiment");
                String confidenceStr = extractValue(jsonText, "confidence");
                String explanation = extractValue(jsonText, "explanation");
                String category = extractValue(jsonText, "category");
                
                double confidence = FALLBACK_CONFIDENCE;
                try {
                    confidence = Double.parseDouble(confidenceStr);
                } catch (Exception e) {
                    logger.warn("Failed to parse confidence, using default: {}", e.getMessage());
                }
                
                SentimentResult result = new SentimentResult(
                    sentiment != null ? sentiment : SENTIMENT_NEUTRAL,
                    confidence,
                    explanation != null ? explanation : "Không có giải thích"
                );
                result.setCategory(category);
                
                return result;
            } else {
                // Fallback: simple heuristic-based analysis
                return heuristicSentimentAnalysis(originalText);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing sentiment response: {}", e.getMessage());
            return heuristicSentimentAnalysis(originalText);
        }
    }

    /**
     * Extract JSON from response text
     */
    private String extractJSON(String text) {
        if (text == null) return null;
        
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        
        return null;
    }

    /**
     * Extract value from simple JSON
     */
    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1);
        }
        
        // Try numeric value
        pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(json);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }

    /**
     * Fallback heuristic-based sentiment analysis
     */
    private SentimentResult heuristicSentimentAnalysis(String text) {
        text = text.toLowerCase();
        
        // Vietnamese positive words
        String[] positiveWords = {"tốt", "hay", "đẹp", "ok", "oke", "tuyệt", "xuất sắc", "hài lòng", "yêu thích", "thích", "khuyên dùng"};
        // Vietnamese negative words
        String[] negativeWords = {"tệ", "kém", "thất vọng", "không tốt", "không thích", "không ok", "không đáng", "rất tệ", "kém chất lượng"};
        
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : positiveWords) {
            if (text.contains(word)) positiveCount++;
        }
        
        for (String word : negativeWords) {
            if (text.contains(word)) negativeCount++;
        }
        
        double heuristicConfidence = NEGATIVE_FLAG_THRESHOLD; // 0.7
        
        if (positiveCount > negativeCount) {
            return new SentimentResult(SENTIMENT_POSITIVE, heuristicConfidence, "Phát hiện từ ngữ tích cực");
        } else if (negativeCount > positiveCount) {
            return new SentimentResult(SENTIMENT_NEGATIVE, heuristicConfidence, "Phát hiện từ ngữ tiêu cực");
        } else {
            return new SentimentResult(SENTIMENT_NEUTRAL, 0.6, "Không rõ ràng tích cực hay tiêu cực");
        }
    }
}

