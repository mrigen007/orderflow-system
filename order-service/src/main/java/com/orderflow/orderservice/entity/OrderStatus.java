package com.orderflow.orderservice.entity;

public enum OrderStatus {
    PENDING,        // Order created, awaiting confirmation
    CONFIRMED,      // Payment confirmed
    PROCESSING,     // Being prepared
    SHIPPED,        // Out for delivery
    DELIVERED,      // Completed successfully
    CANCELLED       // Cancelled by user or system
}