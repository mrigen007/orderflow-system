package com.orderflow.orderservice.exception;

import com.orderflow.orderservice.entity.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super(String.format("Invalid status transition from %s to %s", from, to));
    }
}