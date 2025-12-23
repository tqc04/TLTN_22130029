package com.example.payment.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    PAID,           // Payment has been successfully paid
    COMPLETED,      // Payment completed and confirmed
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
