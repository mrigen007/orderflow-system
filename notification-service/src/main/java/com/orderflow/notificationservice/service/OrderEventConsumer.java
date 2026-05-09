package com.orderflow.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.notificationservice.entity.NotificationType;
import com.orderflow.notificationservice.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order-events",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
            @Payload Map<String, Object> eventMap,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {

        log.info("📨 [NOTIFICATION] Received event from partition: {} offset: {}", partition, offset);

        String eventType = (String) eventMap.get("eventType");

        if (eventType == null) {
            log.warn("⚠️ Missing eventType");
            return;
        }

        if ("ORDER_CREATED".equals(eventType)) {

            OrderCreatedEvent event =
                    objectMapper.convertValue(eventMap, OrderCreatedEvent.class);

            log.info("📦 Processing ORDER_CREATED for order: {}", event.getOrderId());

            notificationService.sendNotification(
                    event.getTenantId(),
                    event.getOrderId(),
                    event.getCustomerId(),
                    NotificationType.ORDER_CREATED,
                    "Order Created",
                    "Order created successfully"
            );
        }
    }
}