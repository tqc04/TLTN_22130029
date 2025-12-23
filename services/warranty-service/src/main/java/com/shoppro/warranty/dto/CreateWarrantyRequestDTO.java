package com.shoppro.warranty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateWarrantyRequestDTO {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    private String customerEmail;
    private String customerPhone;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotNull(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    private String productName;
    private String productSku;

    @NotBlank(message = "Issue description is required")
    private String issueDescription;

    private String priority; // LOW, NORMAL, HIGH, URGENT

    // Constructors
    public CreateWarrantyRequestDTO() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
