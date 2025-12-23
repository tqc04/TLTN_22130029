package com.shoppro.warranty.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranty_requests")
@EntityListeners(AuditingEntityListener.class)
public class WarrantyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "request_number", unique = true, nullable = false)
    private String requestNumber;

    @NotNull
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @NotBlank
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @NotBlank
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @NotBlank
    @Column(name = "customer_phone")
    private String customerPhone;

    @NotNull
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotBlank
    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @NotNull
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @NotBlank
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_sku")
    private String productSku;

    @NotBlank
    @Column(name = "issue_description", nullable = false, columnDefinition = "TEXT")
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WarrantyStatus status = WarrantyStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private WarrantyPriority priority = WarrantyPriority.NORMAL;

    @Column(name = "warranty_period_months")
    private Integer warrantyPeriodMonths;

    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    @Column(name = "warranty_start_date")
    private LocalDateTime warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDateTime warrantyEndDate;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(name = "estimated_completion_date")
    private LocalDateTime estimatedCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDateTime actualCompletionDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public WarrantyRequest() {}

    public WarrantyRequest(String requestNumber, String userId, String customerName, String customerEmail,
                          Long orderId, String orderNumber, String productId, String productName,
                          String issueDescription) {
        this.requestNumber = requestNumber;
        this.userId = userId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.productId = productId;
        this.productName = productName;
        this.issueDescription = issueDescription;
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
