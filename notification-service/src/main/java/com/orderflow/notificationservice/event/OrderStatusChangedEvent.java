package com.orderflow.notificationservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderStatusChangedEvent extends OrderEvent {
    private String oldStatus;
    private String newStatus;
    private Long customerId;
}