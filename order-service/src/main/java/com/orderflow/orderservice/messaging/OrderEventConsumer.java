//package com.orderflow.orderservice.messaging;
//
//import com.orderflow.orderservice.event.OrderCancelledEvent;
//import com.orderflow.orderservice.event.OrderCreatedEvent;
//import com.orderflow.orderservice.event.OrderStatusChangedEvent;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class OrderEventConsumer {
//    @KafkaListener(
//            topics = "order-events",
//            groupId = "order-service-group",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void consumeOrderEvent(
//            @Payload Object event,
//            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
//            @Header(KafkaHeaders.OFFSET) long offset
//    ) {
//        log.info("Received event from partition: {} with offset: {}", partition, offset);
//
//        if (event instanceof OrderCreatedEvent orderCreated) {
//            handleOrderCreated(orderCreated);
//        } else if (event instanceof OrderStatusChangedEvent statusChanged) {
//            handleOrderStatusChanged(statusChanged);
//        } else if (event instanceof OrderCancelledEvent cancelled) {
//            handleOrderCancelled(cancelled);
//        } else {
//            log.warn("Unknown event type: {}", event.getClass().getName());
//        }
//    }
//
//    private void handleOrderCreated(OrderCreatedEvent event) {
//        log.info(" ORDER CREATED - Order ID: {}, Customer: {}, Total: ${}",
//                event.getOrderId(),
//                event.getCustomerId(),
//                event.getTotalAmount());
//
//        // Simulate different microservices processing:
//
//        // 1. Notification Service - Send email
//        log.info("📧 [NOTIFICATION SERVICE] Sending order confirmation email to customer {}",
//                event.getCustomerId());
//
//        // 2. Inventory Service - Reserve stock
//        log.info(" [INVENTORY SERVICE] Reserving stock for {} items",
//                event.getItems().size());
//
//        // 3. Analytics Service - Track metrics
//        log.info("📊 [ANALYTICS SERVICE] Recording order creation event");
//
//        // 4. Loyalty Service - Award points
//        log.info("🎁 [LOYALTY SERVICE] Calculating points for customer {}",
//                event.getCustomerId());
//    }
//
//    private void handleOrderStatusChanged(OrderStatusChangedEvent event) {
//        log.info("🔄 ORDER STATUS CHANGED - Order ID: {}, {} → {}",
//                event.getOrderId(),
//                event.getOldStatus(),
//                event.getNewStatus());
//
//        // Different actions based on new status
//        switch (event.getNewStatus()) {
//            case CONFIRMED -> {
//                log.info(" [PAYMENT SERVICE] Processing payment for order {}",
//                        event.getOrderId());
//                log.info("📧 [NOTIFICATION SERVICE] Sending confirmation email");
//            }
//            case PROCESSING -> {
//                log.info("🏭 [WAREHOUSE SERVICE] Preparing order {} for shipment",
//                        event.getOrderId());
//            }
//            case SHIPPED -> {
//                log.info("🚚 [SHIPPING SERVICE] Order {} dispatched",
//                        event.getOrderId());
//                log.info("📧 [NOTIFICATION SERVICE] Sending tracking email");
//            }
//            case DELIVERED -> {
//                log.info(" [DELIVERY SERVICE] Order {} delivered",
//                        event.getOrderId());
//                log.info("⭐ [REVIEW SERVICE] Requesting customer feedback");
//            }
//        }
//    }
//
//    private void handleOrderCancelled(OrderCancelledEvent event) {
//        log.info(" ORDER CANCELLED - Order ID: {}, Reason: {}",
//                event.getOrderId(),
//                event.getCancellationReason());
//
//        // 1. Payment Service - Refund
//        log.info("💰 [PAYMENT SERVICE] Initiating refund for order {}",
//                event.getOrderId());
//
//        // 2. Inventory Service - Release stock
//        log.info(" [INVENTORY SERVICE] Releasing reserved stock");
//
//        // 3. Notification Service - Send cancellation email
//        log.info("📧 [NOTIFICATION SERVICE] Sending cancellation confirmation");
//    }
//}