package com.example.ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_requests")
public class AIRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_provider", nullable = false)
    private AIProvider aiProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private AIRequestType requestType;

    @Column(name = "prompt", columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public AIRequest() {}

    public AIRequest(String userId, AIProvider aiProvider, AIRequestType requestType, String prompt) {
        this.userId = userId;
        this.aiProvider = aiProvider;
        this.requestType = requestType;
        this.prompt = prompt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public AIProvider getAiProvider() { return aiProvider; }
    public void setAiProvider(AIProvider aiProvider) { this.aiProvider = aiProvider; }

    public AIRequestType getRequestType() { return requestType; }
    public void setRequestType(AIRequestType requestType) { this.requestType = requestType; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
