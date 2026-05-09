package com.orderflow.orderservice.dto.request;

import com.orderflow.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderRequest(

        @NotNull(message = "Order status is required")
        OrderStatus orderStatus
) {}