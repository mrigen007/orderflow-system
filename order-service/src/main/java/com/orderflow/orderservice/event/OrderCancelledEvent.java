package com.orderflow.orderservice.event;

import com.orderflow.orderservice.entity.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderCancelledEvent extends OrderEvent {
    private Long customerId;
    private OrderStatus previousStatus;
    private String cancellationReason;

    public OrderCancelledEvent(Long orderId,
                               String tenantId,
                               Long customerId,
                               OrderStatus previousStatus,
                               String cancellationReason) {

        super("ORDER_CANCELLED", orderId, tenantId);

        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.cancellationReason = cancellationReason;
    }
}