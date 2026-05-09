package com.orderflow.orderservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderCreatedEvent extends OrderEvent {
    private Long customerId;
    private String orderStatus;
    private BigDecimal totalAmount;
    private List<OrderItemData> items;

    @Data
    @NoArgsConstructor
    public static class OrderItemData {
        private Long productId;
        private Integer quantity;
    }
}