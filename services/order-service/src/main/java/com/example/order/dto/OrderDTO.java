package com.example.order.dto;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class OrderDTO {
    private Long id;
    private String orderNumber;
    private String userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private String voucherCode;
    private Long voucherId;
    private String shippingAddress;
    private String paymentMethod;
    private List<OrderItemDTO> orderItems = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderDTO from(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.id = order.getId();
        dto.orderNumber = order.getOrderNumber();
        dto.userId = order.getUserId();
        dto.status = order.getStatus();
        dto.totalAmount = order.getTotalAmount();
        dto.subtotal = order.getSubtotal();
        dto.taxAmount = order.getTaxAmount();
        dto.shippingFee = order.getShippingFee();
        dto.discountAmount = order.getDiscountAmount();
        dto.voucherCode = order.getVoucherCode();
        dto.voucherId = order.getVoucherId();
        dto.shippingAddress = order.getShippingAddress();
        dto.paymentMethod = order.getPaymentMethod();
        dto.createdAt = order.getCreatedAt();
        dto.updatedAt = order.getUpdatedAt();
        
        // Map order items to DTOs to break circular reference
        try {
            if (order.getOrderItems() != null) {
                for (com.example.order.entity.OrderItem item : order.getOrderItems()) {
                    OrderItemDTO itemDto = new OrderItemDTO();
                    itemDto.setId(item.getId());
                    itemDto.setProductId(item.getProductId());
                    itemDto.setProductName(item.getProductName());
                    itemDto.setProductImage(item.getProductImage());
                    itemDto.setProductSku(item.getProductSku() != null ? item.getProductSku() : "");
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setUnitPrice(item.getUnitPrice());
                    itemDto.setPrice(item.getTotalPrice()); // For frontend compatibility
                    itemDto.setTotalPrice(item.getTotalPrice());
                    dto.orderItems.add(itemDto);
                }
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // Handle lazy loading exception - orderItems will be empty
            // This can happen when accessing collections outside of transaction
        }
        
        return dto;
    }

    public Order toEntity() {
        Order o = new Order();
        o.setId(this.id);
        o.setOrderNumber(this.orderNumber);
        o.setUserId(this.userId);
        o.setStatus(this.status != null ? this.status : OrderStatus.PENDING);
        o.setShippingAddress(this.shippingAddress);
        o.setPaymentMethod(this.paymentMethod);
        o.setVoucherCode(this.voucherCode);
        o.setVoucherId(this.voucherId);
        o.setDiscountAmount(this.discountAmount);
        // totalAmount will be calculated server-side, don't trust client
        return o;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public Long getVoucherId() { return voucherId; }
    public void setVoucherId(Long voucherId) { this.voucherId = voucherId; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<OrderItemDTO> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItemDTO> orderItems) { this.orderItems = orderItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


