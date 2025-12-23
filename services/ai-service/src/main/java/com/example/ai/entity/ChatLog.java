package com.example.ai.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_session_id", columnList = "sessionId"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_is_product_related", columnList = "isProductRelated")
})
@EntityListeners(AuditingEntityListener.class)
public class ChatLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String userId;
    
    @Column(nullable = false, length = 100)
    private String sessionId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;
    
    @Column(columnDefinition = "TEXT")
    private String aiResponse;
    
    @Column(nullable = false)
    private Boolean isProductRelated = false;
    
    @Column(columnDefinition = "TEXT")
    private String productIds; // JSON array of product IDs if products found
    
    @Column(columnDefinition = "TEXT")
    private String productNames; // JSON array of product names for quick reference
    
    @Column(nullable = false)
    private Boolean usedAI = false; // Whether AI was used or just database response
    
    @Column(nullable = false)
    private Boolean foundProducts = false; // Whether products were found in database
    
    @Column(length = 50)
    private String responseSource; // "DATABASE", "AI", "FALLBACK", "NO_PRODUCTS"
    
    @Column(columnDefinition = "TEXT")
    private String feedback; // User feedback (helpful, not helpful, etc.)
    
    @Column
    private Integer rating; // 1-5 rating from user
    
    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public ChatLog() {}
    
    public ChatLog(String userId, String sessionId, String userMessage) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.userMessage = userMessage;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public String getAiResponse() {
        return aiResponse;
    }
    
    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
    
    public Boolean getIsProductRelated() {
        return isProductRelated;
    }
    
    public void setIsProductRelated(Boolean isProductRelated) {
        this.isProductRelated = isProductRelated;
    }
    
    public String getProductIds() {
        return productIds;
    }
    
    public void setProductIds(String productIds) {
        this.productIds = productIds;
    }
    
    public String getProductNames() {
        return productNames;
    }
    
    public void setProductNames(String productNames) {
        this.productNames = productNames;
    }
    
    public Boolean getUsedAI() {
        return usedAI;
    }
    
    public void setUsedAI(Boolean usedAI) {
        this.usedAI = usedAI;
    }
    
    public Boolean getFoundProducts() {
        return foundProducts;
    }
    
    public void setFoundProducts(Boolean foundProducts) {
        this.foundProducts = foundProducts;
    }
    
    public String getResponseSource() {
        return responseSource;
    }
    
    public void setResponseSource(String responseSource) {
        this.responseSource = responseSource;
    }
    
    public String getFeedback() {
        return feedback;
    }
    
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

