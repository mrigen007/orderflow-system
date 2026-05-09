package com.orderflow.orderservice.dto.response;

import com.orderflow.orderservice.saga.SagaState;
import com.orderflow.orderservice.saga.SagaStep;
import java.time.LocalDateTime;
import java.util.List;

public record SagaStatusDTO(
        String sagaId,
        Long orderId,
        SagaState state,
        List<SagaStep> completedSteps,
        List<SagaStep> compensatedSteps,
        SagaStep failedStep,
        String failureReason,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String paymentTransactionId,
        String inventoryReservationId,
        String shipmentId
) {}