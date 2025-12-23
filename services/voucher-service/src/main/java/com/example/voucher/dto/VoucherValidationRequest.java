package com.example.voucher.dto;

import java.math.BigDecimal;
import java.util.List;

public class VoucherValidationRequest {
    private String voucherCode;
    private String userId;
    private BigDecimal orderAmount;
    private List<VoucherItem> items;

    public VoucherValidationRequest() {}

    public VoucherValidationRequest(String voucherCode, String userId, BigDecimal orderAmount, List<VoucherItem> items) {
        this.voucherCode = voucherCode;
        this.userId = userId;
        this.orderAmount = orderAmount;
        this.items = items;
    }

    // Getters and Setters
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }

    public List<VoucherItem> getItems() { return items; }
    public void setItems(List<VoucherItem> items) { this.items = items; }

    public static class VoucherItem {
        private String productId;
        private String productName;
        private Long categoryId;
        private Long brandId;
        private BigDecimal price;
        private Integer quantity;

        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

        public Long getBrandId() { return brandId; }
        public void setBrandId(Long brandId) { this.brandId = brandId; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
