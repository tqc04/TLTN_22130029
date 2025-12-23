package com.shoppro.warranty.dto;

import com.shoppro.warranty.entity.WarrantyPriority;
import com.shoppro.warranty.entity.WarrantyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class WarrantyRequestDTO {

    private Long id;
    private String requestNumber;

    @NotNull
    private String userId;

    @NotBlank
    private String customerName;

    @NotBlank
    private String customerEmail;
    private String customerPhone;

    @NotNull
    private Long orderId;

    @NotBlank
    private String orderNumber;

    @NotNull
    private String productId;

    @NotBlank
    private String productName;
    private String productSku;

    @NotBlank
    private String issueDescription;

    private WarrantyStatus status;
    private WarrantyPriority priority;
    private Integer warrantyPeriodMonths;
    private LocalDateTime purchaseDate;
    private LocalDateTime warrantyStartDate;
    private LocalDateTime warrantyEndDate;
    private String resolutionNotes;
    private String technicianNotes;
    private LocalDateTime estimatedCompletionDate;
    private LocalDateTime actualCompletionDate;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public WarrantyRequestDTO() {}

    // Static factory method to create DTO from Entity
    public static WarrantyRequestDTO from(com.shoppro.warranty.entity.WarrantyRequest entity) {
        WarrantyRequestDTO dto = new WarrantyRequestDTO();
        dto.id = entity.getId();
        dto.requestNumber = entity.getRequestNumber();
        dto.userId = entity.getUserId();
        dto.customerName = entity.getCustomerName();
        dto.customerEmail = entity.getCustomerEmail();
        dto.customerPhone = entity.getCustomerPhone();
        dto.orderId = entity.getOrderId();
        dto.orderNumber = entity.getOrderNumber();
        dto.productId = entity.getProductId();
        dto.productName = entity.getProductName();
        dto.productSku = entity.getProductSku();
        dto.issueDescription = entity.getIssueDescription();
        dto.status = entity.getStatus();
        dto.priority = entity.getPriority();
        dto.warrantyPeriodMonths = entity.getWarrantyPeriodMonths();
        dto.purchaseDate = entity.getPurchaseDate();
        dto.warrantyStartDate = entity.getWarrantyStartDate();
        dto.warrantyEndDate = entity.getWarrantyEndDate();
        dto.resolutionNotes = entity.getResolutionNotes();
        dto.technicianNotes = entity.getTechnicianNotes();
        dto.estimatedCompletionDate = entity.getEstimatedCompletionDate();
        dto.actualCompletionDate = entity.getActualCompletionDate();
        dto.rejectionReason = entity.getRejectionReason();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
    }

    // Convert DTO to Entity
    public com.shoppro.warranty.entity.WarrantyRequest toEntity() {
        com.shoppro.warranty.entity.WarrantyRequest entity = new com.shoppro.warranty.entity.WarrantyRequest();
        entity.setId(this.id);
        entity.setRequestNumber(this.requestNumber);
        entity.setUserId(this.userId);
        entity.setCustomerName(this.customerName);
        entity.setCustomerEmail(this.customerEmail);
        entity.setCustomerPhone(this.customerPhone);
        entity.setOrderId(this.orderId);
        entity.setOrderNumber(this.orderNumber);
        entity.setProductId(this.productId);
        entity.setProductName(this.productName);
        entity.setProductSku(this.productSku);
        entity.setIssueDescription(this.issueDescription);
        entity.setStatus(this.status != null ? this.status : WarrantyStatus.PENDING);
        entity.setPriority(this.priority != null ? this.priority : WarrantyPriority.NORMAL);
        entity.setWarrantyPeriodMonths(this.warrantyPeriodMonths);
        entity.setPurchaseDate(this.purchaseDate);
        entity.setWarrantyStartDate(this.warrantyStartDate);
        entity.setWarrantyEndDate(this.warrantyEndDate);
        entity.setResolutionNotes(this.resolutionNotes);
        entity.setTechnicianNotes(this.technicianNotes);
        entity.setEstimatedCompletionDate(this.estimatedCompletionDate);
        entity.setActualCompletionDate(this.actualCompletionDate);
        entity.setRejectionReason(this.rejectionReason);
        return entity;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }

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

    public WarrantyStatus getStatus() { return status; }
    public void setStatus(WarrantyStatus status) { this.status = status; }

    public WarrantyPriority getPriority() { return priority; }
    public void setPriority(WarrantyPriority priority) { this.priority = priority; }

    public Integer getWarrantyPeriodMonths() { return warrantyPeriodMonths; }
    public void setWarrantyPeriodMonths(Integer warrantyPeriodMonths) { this.warrantyPeriodMonths = warrantyPeriodMonths; }

    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }

    public LocalDateTime getWarrantyStartDate() { return warrantyStartDate; }
    public void setWarrantyStartDate(LocalDateTime warrantyStartDate) { this.warrantyStartDate = warrantyStartDate; }

    public LocalDateTime getWarrantyEndDate() { return warrantyEndDate; }
    public void setWarrantyEndDate(LocalDateTime warrantyEndDate) { this.warrantyEndDate = warrantyEndDate; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getTechnicianNotes() { return technicianNotes; }
    public void setTechnicianNotes(String technicianNotes) { this.technicianNotes = technicianNotes; }

    public LocalDateTime getEstimatedCompletionDate() { return estimatedCompletionDate; }
    public void setEstimatedCompletionDate(LocalDateTime estimatedCompletionDate) { this.estimatedCompletionDate = estimatedCompletionDate; }

    public LocalDateTime getActualCompletionDate() { return actualCompletionDate; }
    public void setActualCompletionDate(LocalDateTime actualCompletionDate) { this.actualCompletionDate = actualCompletionDate; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
