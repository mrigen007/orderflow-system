// order-service/src/main/java/com/orderflow/orderservice/event/OrderEvent.java
package com.orderflow.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class OrderEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private Long orderId;
    private String tenantId;

    public OrderEvent(String eventType, Long orderId, String tenantId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.orderId = orderId;
        this.tenantId = tenantId;
    }
}
