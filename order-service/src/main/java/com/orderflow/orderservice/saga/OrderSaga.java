package com.orderflow.orderservice.saga;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of an order fulfillment saga
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSaga {

    private String sagaId;
    private Long orderId;
    private String tenantId;
    private Long customerId;
    private BigDecimal amount;

    @Builder.Default
    private SagaState state = SagaState.STARTED;

    @Builder.Default
    private List<SagaStep> completedSteps = new ArrayList<>();

    @Builder.Default
    private List<SagaStep> compensatedSteps = new ArrayList<>();

    private SagaStep failedStep;
    private String failureReason;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Transaction IDs from external services
    private String paymentTransactionId;
    private String inventoryReservationId;
    private String shipmentId;

    // ===== Getters =====

    public String getSagaId() {
        return sagaId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SagaState getState() {
        return state;
    }

    public List<SagaStep> getCompletedSteps() {
        return completedSteps;
    }

    public List<SagaStep> getCompensatedSteps() {
        return compensatedSteps;
    }

    public SagaStep getFailedStep() {
        return failedStep;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public String getInventoryReservationId() {
        return inventoryReservationId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    // ===== Setters =====

    public void setSagaId(String sagaId) {
        this.sagaId = sagaId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setState(SagaState state) {
        this.state = state;
    }

    public void setCompletedSteps(List<SagaStep> completedSteps) {
        this.completedSteps = completedSteps;
    }

    public void setCompensatedSteps(List<SagaStep> compensatedSteps) {
        this.compensatedSteps = compensatedSteps;
    }

    public void setFailedStep(SagaStep failedStep) {
        this.failedStep = failedStep;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public void setInventoryReservationId(String inventoryReservationId) {
        this.inventoryReservationId = inventoryReservationId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    // ===== Business Methods =====

    public void addCompletedStep(SagaStep step) {
        if (completedSteps == null) {
            completedSteps = new ArrayList<>();
        }
        if (!completedSteps.contains(step)) {
            completedSteps.add(step);
        }
    }

    public void addCompensatedStep(SagaStep step) {
        if (compensatedSteps == null) {
            compensatedSteps = new ArrayList<>();
        }
        if (!compensatedSteps.contains(step)) {
            compensatedSteps.add(step);
        }
    }

    public boolean isCompleted() {
        return state == SagaState.COMPLETED;
    }

    public boolean isFailed() {
        return state == SagaState.FAILED || state == SagaState.COMPENSATED;
    }

    public List<SagaStep> getStepsToCompensate() {
        List<SagaStep> steps = new ArrayList<>(completedSteps != null ? completedSteps : new ArrayList<>());
        steps.sort((a, b) -> Integer.compare(b.getOrder(), a.getOrder()));
        return steps;
    }
}