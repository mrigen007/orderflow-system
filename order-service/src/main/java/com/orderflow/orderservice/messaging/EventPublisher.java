package com.orderflow.orderservice.messaging;

import com.orderflow.orderservice.entity.Order;
import com.orderflow.orderservice.event.OrderCreatedEvent;
import com.orderflow.orderservice.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "order-events";

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("ORDER_CREATED");
        event.setTimestamp(LocalDateTime.now());
        event.setOrderId(order.getId());
        event.setTenantId(order.getTenantId());
        event.setCustomerId(order.getCustomerId());
        event.setOrderStatus(order.getOrderStatus().name());
        event.setTotalAmount(order.getTotalAmount());

        List<OrderCreatedEvent.OrderItemData> eventItems = order.getItems().stream()
                .map(item -> {
                    OrderCreatedEvent.OrderItemData data = new OrderCreatedEvent.OrderItemData();
                    data.setProductId(item.getProductId());
                    data.setQuantity(item.getQuantity());
                    return data;
                })
                .toList();
        event.setItems(eventItems);

        publishEvent(event);
    }

    private void publishEvent(OrderEvent event) {
        log.info("Publishing event: {} for order: {}", event.getEventType(), event.getOrderId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: {} to partition: {} with offset: {}",
                        event.getEventType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish event: {}", event.getEventType(), ex);
            }
        });
    }
}