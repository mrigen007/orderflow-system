package com.orderflow.notificationservice.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderCancelledEvent extends OrderEvent {
    private Long customerId;
    private String previousStatus;
    private String cancellationReason;
}