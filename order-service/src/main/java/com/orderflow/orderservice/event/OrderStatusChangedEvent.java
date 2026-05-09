package com.orderflow.orderservice.event;

import com.orderflow.orderservice.entity.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderStatusChangedEvent extends OrderEvent {
    private OrderStatus oldStatus;
    private OrderStatus newStatus;
    private Long customerId;

    public OrderStatusChangedEvent(Long orderId,
                                   String tenantId,
                                   OrderStatus oldStatus,
                                   OrderStatus newStatus,
                                   Long customerId) {

        super("ORDER_STATUS_CHANGED", orderId, tenantId);

        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.customerId = customerId;
    }
}