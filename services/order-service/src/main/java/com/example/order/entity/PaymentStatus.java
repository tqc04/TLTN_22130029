package com.example.order.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    PAID,           // Payment has been successfully paid
    COMPLETED,      // Payment completed and confirmed
    FAILED,
    CANCELLED,
    REFUNDED
}
