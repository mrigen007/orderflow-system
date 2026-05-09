package com.orderflow.notificationservice.entity;

public enum NotificationType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_PROCESSING,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED
}