package com.orderflow.inventoryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.inventoryservice.dto.ReserveStockRequest;
import com.orderflow.inventoryservice.event.OrderCancelledEvent;
import com.orderflow.inventoryservice.event.OrderCreatedEvent;
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

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "order-events",
            groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
            @Payload Map<String, Object> eventMap,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {

        log.info("[INVENTORY] Received event from partition: {} offset: {}", partition, offset);

        String eventType = (String) eventMap.get("eventType");

        if (eventType == null) {
            log.warn("Event without eventType field: {}", eventMap);
            return;
        }

        try {
            switch (eventType) {

                case "ORDER_CREATED" -> {
                    OrderCreatedEvent event =
                            objectMapper.convertValue(eventMap, OrderCreatedEvent.class);

                    log.info("Processing ORDER_CREATED for order: {}", event.getOrderId());

                    inventoryService.initializeSampleProducts(event.getTenantId());

                    //RESERVE STOCK FOR EACH ITEM
                    if (event.getItems() != null) {
                        for (OrderCreatedEvent.OrderItemData item : event.getItems()) {
                            try {
                                log.info("Reserving stock: Product={} Qty={}",
                                        item.getProductId(), item.getQuantity());

                                ReserveStockRequest request = new ReserveStockRequest(
                                        item.getProductId(),
                                        item.getQuantity(),
                                        event.getOrderId()
                                );

                                inventoryService.reserveStock(event.getTenantId(), request);

                                log.info("Reserved: Product={} Qty={}",
                                        item.getProductId(), item.getQuantity());

                            } catch (Exception e) {
                                log.error("Failed to reserve product {}: {}",
                                        item.getProductId(), e.getMessage());
                            }
                        }
                    } else {
                        log.warn("No items in ORDER_CREATED event for order: {}",
                                event.getOrderId());
                    }
                }

                case "ORDER_CANCELLED" -> {
                    OrderCancelledEvent event =
                            objectMapper.convertValue(eventMap, OrderCancelledEvent.class);

                    log.info("Processing ORDER_CANCELLED for order: {}", event.getOrderId());
                }

                default -> log.warn("Unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }
}