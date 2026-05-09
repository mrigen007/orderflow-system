package com.orderflow.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveStockRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotNull Long orderId
) {}