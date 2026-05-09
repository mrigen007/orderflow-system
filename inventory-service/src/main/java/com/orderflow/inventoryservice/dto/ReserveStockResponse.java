package com.orderflow.inventoryservice.dto;

public record ReserveStockResponse(
        String reservationId,
        Long productId,
        Integer quantity,
        String status,
        String message
) {}