package com.example.recommendation.dto;

import java.math.BigDecimal;

public class ProductRecommendation {
    private String productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Double score;
    private String reason;
    private String type;
    
    public ProductRecommendation() {}
    
    public ProductRecommendation(String productId, String productName, String productImage, 
                               BigDecimal price, Double score, String reason, String type) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.price = price;
        this.score = score;
        this.reason = reason;
        this.type = type;
    }
    
    // Getters and Setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductImage() {
        return productImage;
    }
    
    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Double getScore() {
        return score;
    }
    
    public void setScore(Double score) {
        this.score = score;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
}
