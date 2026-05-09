package com.orderflow.orderservice.saga;

import com.orderflow.orderservice.saga.OrderSaga;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the order fulfillment saga
 * Coordinates steps across multiple services and handles compensation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator {

    // In-memory saga storage (use database in production)
    private final Map<String, OrderSaga> sagaStore = new ConcurrentHashMap<>();

    /**
     * Start a new order fulfillment saga
     */
    public OrderSaga startOrderSaga(Long orderId, String tenantId, Long customerId, BigDecimal amount) {
        String sagaId = UUID.randomUUID().toString();

        OrderSaga saga = OrderSaga.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .tenantId(tenantId)
                .customerId(customerId)
                .amount(amount)
                .state(SagaState.STARTED)
                .startedAt(LocalDateTime.now())
                .build();

        sagaStore.put(sagaId, saga);

        log.info("🎬 Saga started: {} for order: {} (tenant: {})", sagaId, orderId, tenantId);

        return saga;
    }

    /**
     * Execute the saga - orchestrate all steps
     */
    public void executeSaga(String sagaId) {
        OrderSaga saga = sagaStore.get(sagaId);
        if (saga == null) {
            log.error("❌ Saga not found: {}", sagaId);
            return;
        }

        try {
            // Step 1: Create Order (already done by OrderService)
            executeCreateOrder(saga);

            // Step 2: Process Payment
            executeProcessPayment(saga);

            // Step 3: Reserve Inventory
            executeReserveInventory(saga);

            // Step 4: Create Shipment
            executeCreateShipment(saga);

            // All steps successful
            completeSaga(saga);

        } catch (SagaException e) {
            log.error("❌ Saga step failed: {} - {}", e.getStep(), e.getMessage());
            compensateSaga(saga, e.getStep(), e.getMessage());
        }
    }

    // =====================================
    // Saga Steps (Forward Execution)
    // =====================================

    private void executeCreateOrder(OrderSaga saga) throws SagaException {
        log.info("📝 Step 1: Create Order - Order ID: {}", saga.getOrderId());

        try {
            // Order already created by OrderService
            saga.addCompletedStep(SagaStep.CREATE_ORDER);
            saga.setState(SagaState.ORDER_CREATED);

            log.info("✅ Step 1 complete: Order created");

        } catch (Exception e) {
            throw new SagaException(SagaStep.CREATE_ORDER, "Failed to create order: " + e.getMessage());
        }
    }

    private void executeProcessPayment(OrderSaga saga) throws SagaException {
        log.info("💳 Step 2: Process Payment - Amount: ${}", saga.getAmount());

        try {
            // Simulate payment processing (call external payment service)
            String transactionId = simulatePaymentService(saga);

            saga.setPaymentTransactionId(transactionId);
            saga.addCompletedStep(SagaStep.PROCESS_PAYMENT);
            saga.setState(SagaState.PAYMENT_PROCESSED);

            log.info("✅ Step 2 complete: Payment processed - Transaction: {}", transactionId);

        } catch (Exception e) {
            throw new SagaException(SagaStep.PROCESS_PAYMENT, "Payment failed: " + e.getMessage());
        }
    }

    private void executeReserveInventory(OrderSaga saga) throws SagaException {
        log.info("📦 Step 3: Reserve Inventory - Order ID: {}", saga.getOrderId());

        try {
            // Simulate inventory reservation (call external inventory service)
            String reservationId = simulateInventoryService(saga);

            saga.setInventoryReservationId(reservationId);
            saga.addCompletedStep(SagaStep.RESERVE_INVENTORY);
            saga.setState(SagaState.INVENTORY_RESERVED);

            log.info("✅ Step 3 complete: Inventory reserved - Reservation: {}", reservationId);

        } catch (Exception e) {
            throw new SagaException(SagaStep.RESERVE_INVENTORY, "Inventory reservation failed: " + e.getMessage());
        }
    }

    private void executeCreateShipment(OrderSaga saga) throws SagaException {
        log.info("🚚 Step 4: Create Shipment - Order ID: {}", saga.getOrderId());

        try {
            // Simulate shipment creation (call external shipping service)
            String shipmentId = simulateShippingService(saga);

            saga.setShipmentId(shipmentId);
            saga.addCompletedStep(SagaStep.CREATE_SHIPMENT);

            log.info("✅ Step 4 complete: Shipment created - Shipment: {}", shipmentId);

        } catch (Exception e) {
            throw new SagaException(SagaStep.CREATE_SHIPMENT, "Shipment creation failed: " + e.getMessage());
        }
    }

    // =====================================
    // Saga Compensation (Rollback)
    // =====================================

    private void compensateSaga(OrderSaga saga, SagaStep failedStep, String reason) {
        log.warn("⚠️ Compensating saga: {} - Failed at step: {}", saga.getSagaId(), failedStep);

        saga.setFailedStep(failedStep);
        saga.setFailureReason(reason);
        saga.setState(SagaState.COMPENSATING);

        // Compensate in reverse order
        for (SagaStep step : saga.getStepsToCompensate()) {
            try {
                compensateStep(saga, step);
            } catch (Exception e) {
                log.error("❌ Compensation failed for step: {} - {}", step, e.getMessage());
                // Continue compensating other steps
            }
        }

        saga.setState(SagaState.COMPENSATED);
        saga.setCompletedAt(LocalDateTime.now());

        log.info("🔄 Saga compensated: {}", saga.getSagaId());
    }

    private void compensateStep(OrderSaga saga, SagaStep step) {
        log.info("🔙 Compensating step: {}", step);

        switch (step) {
            case CREATE_SHIPMENT -> {
                if (saga.getShipmentId() != null) {
                    log.info("🔙 Cancelling shipment: {}", saga.getShipmentId());
                    // Call shipping service to cancel shipment
                    saga.addCompensatedStep(step);
                }
            }
            case RESERVE_INVENTORY -> {
                if (saga.getInventoryReservationId() != null) {
                    log.info("🔙 Releasing inventory: {}", saga.getInventoryReservationId());
                    // Call inventory service to release reservation
                    saga.addCompensatedStep(step);
                }
            }
            case PROCESS_PAYMENT -> {
                if (saga.getPaymentTransactionId() != null) {
                    log.info("🔙 Refunding payment: {}", saga.getPaymentTransactionId());
                    // Call payment service to refund
                    saga.addCompensatedStep(step);
                }
            }
            case CREATE_ORDER -> {
                log.info("🔙 Cancelling order: {}", saga.getOrderId());
                // Call order service to cancel order
                saga.addCompensatedStep(step);
            }
        }
    }

    private void completeSaga(OrderSaga saga) {
        saga.setState(SagaState.COMPLETED);
        saga.setCompletedAt(LocalDateTime.now());

        log.info("🎉 Saga completed successfully: {} - Order: {}", saga.getSagaId(), saga.getOrderId());
    }

    // =====================================
    // Simulated External Services
    // (In real system, these would be HTTP/gRPC calls)
    // =====================================

    private String simulatePaymentService(OrderSaga saga) {
        // Simulate payment processing
        log.info("💳 [PAYMENT SERVICE] Processing payment of ${} for customer {}",
                saga.getAmount(), saga.getCustomerId());

        // Simulate random failure (10% chance)
        if (Math.random() < 0.1) {
            throw new RuntimeException("Payment declined - insufficient funds");
        }

        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String simulateInventoryService(OrderSaga saga) {
        // Simulate inventory check
        log.info("📦 [INVENTORY SERVICE] Reserving stock for order {}", saga.getOrderId());

        // Simulate random failure (15% chance)
        if (Math.random() < 0.15) {
            throw new RuntimeException("Out of stock");
        }

        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String simulateShippingService(OrderSaga saga) {
        // Simulate shipment creation
        log.info("🚚 [SHIPPING SERVICE] Creating shipment for order {}", saga.getOrderId());

        // Simulate random failure (5% chance)
        if (Math.random() < 0.05) {
            throw new RuntimeException("Shipping service unavailable");
        }

        return "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // =====================================
    // Saga Query Methods
    // =====================================

    public OrderSaga getSaga(String sagaId) {
        return sagaStore.get(sagaId);
    }

    public OrderSaga getSagaByOrderId(Long orderId) {
        return sagaStore.values().stream()
                .filter(saga -> saga.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
    }
}