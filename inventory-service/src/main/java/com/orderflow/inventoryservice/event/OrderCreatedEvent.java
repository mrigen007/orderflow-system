package com.orderflow.inventoryservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent extends OrderEvent {
    private Long customerId;
    private String orderStatus;
    private List<OrderItemData> items;

    @Data
    @NoArgsConstructor
    public static class OrderItemData {
        private Long productId;
        private Integer quantity;
    }
}