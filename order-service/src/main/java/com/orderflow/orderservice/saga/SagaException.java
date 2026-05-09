package com.orderflow.orderservice.saga;

import lombok.Getter;

@Getter
public class SagaException extends Exception {
    private final SagaStep step;

    public SagaException(SagaStep step, String message) {
        super(message);
        this.step = step;
    }
}