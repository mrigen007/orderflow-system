package com.orderflow.orderservice.controller;

import com.orderflow.orderservice.dto.request.*;
import com.orderflow.orderservice.dto.response.*;
import com.orderflow.orderservice.saga.OrderSaga;
import com.orderflow.orderservice.entity.OrderStatus;
import com.orderflow.orderservice.saga.OrderSagaOrchestrator;
import com.orderflow.orderservice.service.OrderService;
import com.orderflow.orderservice.service.RetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final RetryService retryService;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @GetMapping("/{id}/saga")
    public ResponseEntity<OrderSaga> getOrderSaga(@PathVariable Long id) {
        log.info("REST: Fetching saga for order: {}", id);

        OrderSaga saga = sagaOrchestrator.getSagaByOrderId(id);

        if (saga == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(saga);
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("REST: Creating order for customer: {}", request.customerId());
        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        log.info("REST: Fetching order: {}", id);
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) OrderStatus status
    ) {
        log.info("REST: Fetching orders, customerId: {}, status: {}", customerId, status);

        List<OrderDTO> orders;

        if (customerId != null) {
            orders = orderService.getOrdersByCustomer(customerId);
        } else if (status != null) {
            orders = orderService.getOrdersByStatus(status);
        } else {
            orders = orderService.getAllOrders();
        }

        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        log.info("REST: Updating order {} status to: {}", id, request.orderStatus());
        OrderDTO order = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        log.info("REST: Cancelling order: {}", id);
        OrderDTO order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status-with-retry")
    public ResponseEntity<OrderDTO> updateOrderStatusWithRetry(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        log.info("REST: Updating order {} status with retry to: {}", id, request.orderStatus());
        OrderDTO order = retryService.updateOrderStatusWithRetry(id, request, 3);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status-with-lock")
    public ResponseEntity<OrderDTO> updateOrderStatusWithLock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        log.info("REST: Updating order {} status with lock to: {}", id, request.orderStatus());
        OrderDTO order = orderService.updateOrderStatusWithPessimisticLock(id, request);
        return ResponseEntity.ok(order);
    }
}