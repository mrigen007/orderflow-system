package com.orderflow.orderservice.saga;

public enum SagaState {
    STARTED,
    ORDER_CREATED,
    PAYMENT_PROCESSED,
    INVENTORY_RESERVED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}