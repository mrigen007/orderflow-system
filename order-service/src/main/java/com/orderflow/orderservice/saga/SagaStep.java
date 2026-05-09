package com.orderflow.orderservice.saga;

public enum SagaStep {
    CREATE_ORDER("Create Order", 1),
    PROCESS_PAYMENT("Process Payment", 2),
    RESERVE_INVENTORY("Reserve Inventory", 3),
    CREATE_SHIPMENT("Create Shipment", 4);

    private final String description;
    private final int order;

    SagaStep(String description, int order) {
        this.description = description;
        this.order = order;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }
}