package com.orderflow.orderservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemRequest(

        @NotNull(message = "Product ID is required")
        @Positive
        Long productId,

        @NotBlank(message = "Product name is required")
        String productName,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price
) {}