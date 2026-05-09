package com.orderflow.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class OrderEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long orderId;
    private String tenantId;
}