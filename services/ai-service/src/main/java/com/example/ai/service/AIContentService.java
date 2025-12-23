package com.example.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AIContentService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIContentService.class);
    
    // Token limits for different content types
    private static final int PRODUCT_DESCRIPTION_MAX_TOKENS = 1000;
    private static final int MARKETING_COPY_MAX_TOKENS = 800;
    private static final int SEO_DESCRIPTION_MAX_TOKENS = 300;
    private static final int TAGS_MAX_TOKENS = 300;
    private static final int TITLE_VARIATIONS_MAX_TOKENS = 500;
    
    // SEO limits
    private static final int SEO_DESCRIPTION_MAX_LENGTH = 160;
    private static final int SEO_DESCRIPTION_TRUNCATE_LENGTH = 157;
    
    // Map keys constants
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_TEXT = "text";
    private static final String KEY_ERROR = "error";
    
    @Autowired
    private GeminiService geminiService;

    /**
     * Safely extract text from Gemini API result
     */
    private String extractTextFromResult(Map<String, Object> result) {
        if (result == null) {
            return null;
        }
        
        Object successObj = result.get(KEY_SUCCESS);
        if (successObj instanceof Boolean && (Boolean) successObj) {
            Object textObj = result.get(KEY_TEXT);
            if (textObj instanceof String) {
                return (String) textObj;
            }
        }
        
        return null;
    }

    /**
     * Generate product description
     */
    public String generateProductDescription(String productName, String category, String brand, 
                                             List<String> features, List<String> specifications) {
        // Input validation
        if (productName == null || productName.trim().isEmpty()) {
            logger.warn("Product name is null or empty");
            return "Sản phẩm chất lượng cao";
        }
        
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Tạo mô tả sản phẩm chi tiết, hấp dẫn và SEO-friendly cho:\n\n");
            prompt.append("Tên sản phẩm: ").append(productName).append("\n");
            prompt.append("Danh mục: ").append(category).append("\n");
            prompt.append("Thương hiệu: ").append(brand).append("\n\n");
            
            if (features != null && !features.isEmpty()) {
                prompt.append("Tính năng nổi bật:\n");
                for (String feature : features) {
                    prompt.append("- ").append(feature).append("\n");
                }
                prompt.append("\n");
            }
            
            if (specifications != null && !specifications.isEmpty()) {
                prompt.append("Thông số kỹ thuật:\n");
                for (String spec : specifications) {
                    prompt.append("- ").append(spec).append("\n");
                }
                prompt.append("\n");
            }
            
            prompt.append("Yêu cầu:\n");
            prompt.append("1. Mô tả ngắn gọn, súc tích (khoảng 150-200 từ)\n");
            prompt.append("2. Nhấn mạnh lợi ích cho khách hàng\n");
            prompt.append("3. Sử dụng từ ngữ hấp dẫn, dễ hiểu\n");
            prompt.append("4. Tối ưu cho SEO (bao gồm từ khóa liên quan)\n");
            prompt.append("5. Viết bằng tiếng Việt\n");

            String systemMessage = "You are an expert product description writer for an e-commerce platform. " +
                                  "Create compelling, SEO-optimized product descriptions in Vietnamese.";
            
            Map<String, Object> result = geminiService.generateText(prompt.toString(), systemMessage, PRODUCT_DESCRIPTION_MAX_TOKENS);
            String text = extractTextFromResult(result);
            
            if (text != null && !text.trim().isEmpty()) {
                return text;
            } else {
                Object error = result != null ? result.get(KEY_ERROR) : "Unknown error";
                logger.error("Failed to generate product description: {}", error);
                return generateFallbackDescription(productName, category, brand, features);
            }
            
        } catch (Exception e) {
            logger.error("Error generating product description: {}", e.getMessage(), e);
            return generateFallbackDescription(productName, category, brand, features);
        }
    }

    /**
     * Generate marketing copy for product
     */
    public String generateMarketingCopy(String productName, String targetAudience, String tone) {
        // Input validation
        if (productName == null || productName.trim().isEmpty()) {
            logger.warn("Product name is null or empty for marketing copy");
            return "Sản phẩm chất lượng cao, giá tốt. Mua ngay!";
        }
        
        try {
            String prompt = String.format(
                "Tạo nội dung marketing hấp dẫn cho sản phẩm: %s\n\n" +
                "Đối tượng mục tiêu: %s\n" +
                "Giọng điệu: %s\n\n" +
                "Yêu cầu:\n" +
                "1. Tạo 3 câu slogan ngắn gọn, ấn tượng\n" +
                "2. Viết đoạn quảng cáo 2-3 câu kêu gọi hành động\n" +
                "3. Đề xuất 5 hashtags phù hợp\n" +
                "4. Sử dụng ngôn ngữ phù hợp với đối tượng khách hàng\n" +
                "5. Viết bằng tiếng Việt\n",
                productName,
                targetAudience != null ? targetAudience : "người tiêu dùng trẻ",
                tone != null ? tone : "thân thiện, năng động"
            );

            String systemMessage = "You are a marketing copywriter. Create engaging, persuasive marketing content in Vietnamese.";
            
            Map<String, Object> result = geminiService.generateText(prompt, systemMessage, MARKETING_COPY_MAX_TOKENS);
            String text = extractTextFromResult(result);
            
            if (text != null && !text.trim().isEmpty()) {
                return text;
            } else {
                Object error = result != null ? result.get(KEY_ERROR) : "Unknown error";
                logger.error("Failed to generate marketing copy: {}", error);
                return "Sản phẩm chất lượng cao, giá tốt. Mua ngay!";
            }
            
        } catch (Exception e) {
            logger.error("Error generating marketing copy: {}", e.getMessage(), e);
            return "Sản phẩm chất lượng cao, giá tốt. Mua ngay!";
        }
    }

    /**
     * Generate SEO description (meta description)
     */
    public String generateSEODescription(String productName, String category, String mainKeyword) {
        // Input validation
        if (productName == null || productName.trim().isEmpty()) {
            logger.warn("Product name is null or empty for SEO description");
            return "Sản phẩm chất lượng cao, giá tốt. Mua ngay!";
        }
        
        try {
            String prompt = String.format(
                "Tạo SEO meta description tối ưu cho sản phẩm:\n\n" +
                "Tên sản phẩm: %s\n" +
                "Danh mục: %s\n" +
                "Từ khóa chính: %s\n\n" +
                "Yêu cầu:\n" +
                "1. Độ dài: 150-160 ký tự\n" +
                "2. Bao gồm từ khóa chính\n" +
                "3. Có call-to-action\n" +
                "4. Hấp dẫn, khuyến khích click\n" +
                "5. Viết bằng tiếng Việt\n" +
                "6. CHỈ trả về meta description, không có text khác\n",
                productName,
                category,
                mainKeyword != null ? mainKeyword : productName
            );

            String systemMessage = "You are an SEO expert. Create optimized meta descriptions in Vietnamese.";
            
            Map<String, Object> result = geminiService.generateText(prompt, systemMessage, SEO_DESCRIPTION_MAX_TOKENS);
            String desc = extractTextFromResult(result);
            
            if (desc != null && !desc.trim().isEmpty()) {
                // Trim to max length for SEO
                if (desc.length() > SEO_DESCRIPTION_MAX_LENGTH) {
                    desc = desc.substring(0, SEO_DESCRIPTION_TRUNCATE_LENGTH) + "...";
                }
                return desc;
            } else {
                Object error = result != null ? result.get(KEY_ERROR) : "Unknown error";
                logger.error("Failed to generate SEO description: {}", error);
                return String.format("%s - %s chính hãng, giá tốt. Mua ngay!", productName, category);
            }
            
        } catch (Exception e) {
            logger.error("Error generating SEO description: {}", e.getMessage(), e);
            return String.format("%s - %s chính hãng, giá tốt. Mua ngay!", productName, category);
        }
    }

    /**
     * Suggest product tags
     */
    public List<String> suggestProductTags(String productName, String category, String description) {
        // Input validation
        if (productName == null || productName.trim().isEmpty()) {
            logger.warn("Product name is null or empty for tag suggestion");
            return generateDefaultTags("Unknown", category != null ? category : "General");
        }
        
        try {
            String prompt = String.format(
                "Đề xuất 8-10 tags phù hợp cho sản phẩm:\n\n" +
                "Tên sản phẩm: %s\n" +
                "Danh mục: %s\n" +
                "Mô tả: %s\n\n" +
                "Yêu cầu:\n" +
                "1. Trả về danh sách tags cách nhau bởi dấu phẩy\n" +
                "2. Mỗi tag ngắn gọn, 1-3 từ\n" +
                "3. Liên quan đến sản phẩm, category, hoặc use-case\n" +
                "4. Viết bằng tiếng Việt\n" +
                "5. CHỈ trả về danh sách tags, không có text khác\n",
                productName,
                category,
                description != null ? description.substring(0, Math.min(description.length(), 200)) : ""
            );

            String systemMessage = "You are a product tagging expert. Generate relevant tags in Vietnamese.";
            
            Map<String, Object> result = geminiService.generateText(prompt, systemMessage, TAGS_MAX_TOKENS);
            String tagsText = extractTextFromResult(result);
            
            if (tagsText != null && !tagsText.trim().isEmpty()) {
                List<String> tags = parseTagsList(tagsText);
                if (!tags.isEmpty()) {
                    return tags;
                }
            }
            
            Object error = result != null ? result.get(KEY_ERROR) : "Unknown error";
            logger.error("Failed to suggest product tags: {}", error);
            return generateDefaultTags(productName, category);
            
        } catch (Exception e) {
            logger.error("Error suggesting product tags: {}", e.getMessage(), e);
            return generateDefaultTags(productName, category);
        }
    }

    /**
     * Generate title variations for A/B testing
     */
    public List<String> generateTitleVariations(String productName, int count) {
        // Input validation
        if (productName == null || productName.trim().isEmpty()) {
            logger.warn("Product name is null or empty for title variations");
            List<String> defaultVariations = new ArrayList<>();
            defaultVariations.add("Sản phẩm mới");
            return defaultVariations;
        }
        
        if (count <= 0) {
            count = 3; // Default count
        }
        
        try {
            String prompt = String.format(
                "Tạo %d biến thể tên sản phẩm khác nhau cho A/B testing:\n\n" +
                "Tên gốc: %s\n\n" +
                "Yêu cầu:\n" +
                "1. Mỗi biến thể trên một dòng\n" +
                "2. Giữ nguyên ý nghĩa nhưng diễn đạt khác nhau\n" +
                "3. Một số biến thể ngắn gọn, một số chi tiết hơn\n" +
                "4. Tối ưu cho SEO và click-through rate\n" +
                "5. Viết bằng tiếng Việt\n",
                count,
                productName
            );

            String systemMessage = "You are a product naming expert. Create engaging title variations in Vietnamese.";
            
            Map<String, Object> result = geminiService.generateText(prompt, systemMessage, TITLE_VARIATIONS_MAX_TOKENS);
            String text = extractTextFromResult(result);
            
            if (text != null && !text.trim().isEmpty()) {
                List<String> variations = parseTitleVariations(text, count);
                if (!variations.isEmpty()) {
                    return variations;
                }
            }
            
            Object error = result != null ? result.get(KEY_ERROR) : "Unknown error";
            logger.error("Failed to generate title variations: {}", error);
            List<String> fallbackVariations = new ArrayList<>();
            fallbackVariations.add(productName);
            return fallbackVariations;
            
        } catch (Exception e) {
            logger.error("Error generating title variations: {}", e.getMessage(), e);
            List<String> variations = new ArrayList<>();
            variations.add(productName);
            return variations;
        }
    }

    /**
     * Generate fallback product description
     */
    private String generateFallbackDescription(String productName, String category, String brand, List<String> features) {
        StringBuilder desc = new StringBuilder();
        desc.append(productName).append(" là sản phẩm ").append(category).append(" chính hãng từ thương hiệu ")
            .append(brand).append(".\n\n");
        
        if (features != null && !features.isEmpty()) {
            desc.append("Tính năng nổi bật:\n");
            for (String feature : features) {
                desc.append("• ").append(feature).append("\n");
            }
        }
        
        desc.append("\nSản phẩm chất lượng cao, đảm bảo chính hãng. Mua ngay hôm nay!");
        
        return desc.toString();
    }

    /**
     * Parse tags list from AI response
     */
    private List<String> parseTagsList(String tagsText) {
        List<String> tags = new ArrayList<>();
        
        // Remove common prefixes and clean up
        tagsText = tagsText.replaceAll("(?i)(tags:|danh sách tags:|gợi ý:)", "").trim();
        
        // Split by comma, newline, or semicolon
        String[] parts = tagsText.split("[,\n;]+");
        
        for (String tag : parts) {
            tag = tag.trim();
            if (!tag.isEmpty() && tag.length() <= 50) {
                // Remove bullet points and numbers
                tag = tag.replaceAll("^[\\d.\\-•*]+\\s*", "").trim();
                if (!tag.isEmpty()) {
                    tags.add(tag);
                }
            }
        }
        
        return tags;
    }

    /**
     * Parse title variations from AI response
     */
    private List<String> parseTitleVariations(String text, int count) {
        List<String> variations = new ArrayList<>();
        
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            // Remove numbering, bullet points
            line = line.replaceAll("^[\\d.\\-•*]+\\s*", "").trim();
            
            if (!line.isEmpty() && line.length() <= 200) {
                variations.add(line);
                if (variations.size() >= count) {
                    break;
                }
            }
        }
        
        return variations;
    }

    /**
     * Generate default tags as fallback
     */
    private List<String> generateDefaultTags(String productName, String category) {
        List<String> tags = new ArrayList<>();
        tags.add(category);
        tags.add("chính hãng");
        tags.add("giá tốt");
        tags.add("mới nhất");
        return tags;
    }
}

