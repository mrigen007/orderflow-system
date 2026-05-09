package com.orderflow.orderservice.service;

import com.orderflow.orderservice.dto.request.UpdateOrderRequest;
import com.orderflow.orderservice.dto.response.OrderDTO;
import com.orderflow.orderservice.exception.OptimisticLockingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final OrderService orderService;

    /**
     * Retry order status update if optimistic lock failure occurs
     * Real-world scenario: Two users updating same order simultaneously
     */
    public OrderDTO updateOrderStatusWithRetry(Long id, UpdateOrderRequest request, int maxAttempts) {
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                attempt++;
                log.info("Attempt {} to update order {}", attempt, id);

                OrderDTO result = orderService.updateOrderStatus(id, request);

                log.info("Successfully updated order {} on attempt {}", id, attempt);
                return result;

            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock failure on attempt {} for order {}", attempt, id);

                if (attempt >= maxAttempts) {
                    log.error("Failed to update order {} after {} attempts", id, maxAttempts);
                    throw new OptimisticLockingException(
                            "Failed to update order after " + maxAttempts + " attempts. Please try again."
                    );
                }


                try {
                    long backoffMs = (long) Math.pow(2, attempt) * 100;
                    log.info("Backing off for {}ms before retry", backoffMs);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new OptimisticLockingException("Unexpected retry failure");
    }
}