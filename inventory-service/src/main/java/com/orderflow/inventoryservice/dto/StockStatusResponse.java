package com.orderflow.inventoryservice.dto;

public record StockStatusResponse(
        Long productId,
        String productName,
        Integer totalStock,
        Integer reservedStock,
        Integer availableStock
) {}