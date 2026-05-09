package com.orderflow.orderservice.service;

import com.orderflow.orderservice.dto.request.*;
import com.orderflow.orderservice.dto.response.*;
import com.orderflow.orderservice.entity.*;
import com.orderflow.orderservice.exception.OrderNotFoundException;
import com.orderflow.orderservice.exception.InvalidOrderStatusTransitionException;
import com.orderflow.orderservice.messaging.EventPublisher;
import com.orderflow.orderservice.repository.OrderRepository;
import com.orderflow.orderservice.saga.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import com.orderflow.orderservice.context.TenantContext;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Transactional
    @Caching(
            put = @CachePut(value = "orders", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "ordersByCustomer", key = "#request.customerId"),
                    @CacheEvict(value = "ordersByStatus", key = "T(com.orderflow.orderservice.entity.OrderStatus).PENDING")
            }
    )
    public OrderDTO createOrder(CreateOrderRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating order for customer: {}", request.customerId());

        Order order = Order.builder()
                .tenantId(tenantId)
                .customerId(request.customerId())
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> items = request.items().stream()
                .map(itemReq -> OrderItem.builder()
                        .order(order)
                        .productId(itemReq.productId())
                        .productName(itemReq.productName())
                        .quantity(itemReq.quantity())
                        .price(itemReq.price())
                        .build())
                .collect(Collectors.toList());

        order.setItems(items);

        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", savedOrder.getId());
        publishOrderCreatedEvent(savedOrder);
        OrderSaga saga = sagaOrchestrator.startOrderSaga(
                savedOrder.getId(),
                savedOrder.getTenantId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount()
        );
        new Thread(() -> sagaOrchestrator.executeSaga(saga.getSagaId())).start();
        return convertToDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id", unless = "#result == null")
    public OrderDTO getOrderById(Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching order from database: {}", id);

        Order order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching all orders");

        return orderRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "ordersByCustomer", key = "#customerId")
    public List<OrderDTO> getOrdersByCustomer(Long customerId) {
        String tenantId = TenantContext.getTenantId();

        log.info("Fetching orders from database for customer: {}", customerId);

        return orderRepository.findByTenantIdAndCustomerId(tenantId, customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "ordersByStatus", key = "#status")
    public List<OrderDTO> getOrdersByStatus(OrderStatus status) {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching orders from database with status: {}", status);

        return orderRepository.findByTenantIdAndOrderStatus(tenantId, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    @Transactional
    @Caching(
            put = @CachePut(value = "orders", key = "#id"),
            evict = {
                    @CacheEvict(value = "ordersByCustomer", allEntries = true),
                    @CacheEvict(value = "ordersByStatus", allEntries = true)
            }
    )
    public OrderDTO updateOrderStatus(Long id, UpdateOrderRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating order {} to status: {}", id, request.orderStatus());

        Order order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));
        OrderStatus oldStatus = order.getOrderStatus();
        validateStatusTransition(order.getOrderStatus(), request.orderStatus());

        order.setOrderStatus(request.orderStatus());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to: {}", id, request.orderStatus());
//        publishOrderStatusChangedEvent(updatedOrder, oldStatus);
        return convertToDTO(updatedOrder);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "orders", key = "#id"),
            evict = {
                    @CacheEvict(value = "ordersByCustomer", allEntries = true),
                    @CacheEvict(value = "ordersByStatus", allEntries = true)
            }
    )
    public OrderDTO cancelOrder(Long id) {
        String tenantId = TenantContext.getTenantId();
        log.info("Cancelling order: {}", id);
        Order order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        OrderStatus previousStatus = order.getOrderStatus();
        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusTransitionException(
                    order.getOrderStatus(),
                    OrderStatus.CANCELLED
            );
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);

        log.info("Order {} cancelled successfully", id);
//        publishOrderCancelledEvent(cancelledOrder, previousStatus);
        return convertToDTO(cancelledOrder);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "orders", key = "#id"),
            evict = {
                    @CacheEvict(value = "ordersByCustomer", allEntries = true),
                    @CacheEvict(value = "ordersByStatus", allEntries = true)
            }
    )
    public OrderDTO updateOrderStatusWithPessimisticLock(Long id, UpdateOrderRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating order {} with pessimistic lock to status: {}", id, request.orderStatus());

        Order order = orderRepository.findByIdAndTenantIdWithLock(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        validateStatusTransition(order.getOrderStatus(), request.orderStatus());

        order.setOrderStatus(request.orderStatus());
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated to: {} (with lock)", id, request.orderStatus());

        return convertToDTO(updatedOrder);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED ||
                    newStatus == OrderStatus.CANCELLED;

            case CONFIRMED -> newStatus == OrderStatus.PROCESSING ||
                    newStatus == OrderStatus.CANCELLED;

            case PROCESSING -> newStatus == OrderStatus.SHIPPED ||
                    newStatus == OrderStatus.CANCELLED;

            case SHIPPED -> newStatus == OrderStatus.DELIVERED;

            case DELIVERED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new InvalidOrderStatusTransitionException(currentStatus, newStatus);
        }
    }

    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderDTO(
                order.getId(),
                order.getTenantId(),
                order.getCustomerId(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDTOs
        );
    }

    private void publishOrderCreatedEvent(Order order) {
        eventPublisher.publishOrderCreated(order);
    }

}
