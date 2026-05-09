package com.orderflow.orderservice.dto.response;

import com.orderflow.orderservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
        Long id,
        String tenantId,
        Long customerId,
        OrderStatus orderStatus,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemDTO> items
) {}