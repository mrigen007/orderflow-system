package com.orderflow.orderservice.service;

import com.orderflow.orderservice.dto.request.CreateOrderRequest;
import com.orderflow.orderservice.dto.request.OrderItemRequest;
import com.orderflow.orderservice.dto.request.UpdateOrderRequest;
import com.orderflow.orderservice.dto.response.OrderDTO;
import com.orderflow.orderservice.entity.Order;
import com.orderflow.orderservice.entity.OrderItem;
import com.orderflow.orderservice.entity.OrderStatus;
import com.orderflow.orderservice.exception.InvalidOrderStatusTransitionException;
import com.orderflow.orderservice.exception.OrderNotFoundException;
import com.orderflow.orderservice.repository.OrderRepository;
import com.orderflow.orderservice.context.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        // Set tenant context for all tests
        TenantContext.setTenantId("test-tenant");

        OrderItemRequest itemRequest = new OrderItemRequest(
                1L,
                "Test Product",
                2,
                new BigDecimal("10.00")
        );

        createOrderRequest = new CreateOrderRequest(1L, List.of(itemRequest));

        order = Order.builder()
                .id(1L)
                .tenantId("test-tenant")
                .customerId(1L)
                .orderStatus(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("20.00"))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .order(order)
                .productId(1L)
                .productName("Test Product")
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build();

        order.setItems(List.of(orderItem));
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        OrderDTO result = orderService.createOrder(createOrderRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.customerId()).isEqualTo(1L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualTo(new BigDecimal("20.00"));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        // Given
        when(orderRepository.findByIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.of(order));

        // When
        OrderDTO result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(orderRepository).findByIdAndTenantId(1L, "test-tenant");
    }

    @Test
    void getOrderById_ShouldThrowException_WhenOrderNotFound() {
        // Given
        when(orderRepository.findByIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(1L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with id: 1");
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatusSuccessfully() {
        // Given
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        OrderDTO result = orderService.updateOrderStatus(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_ShouldThrowException_WhenInvalidTransition() {
        // Given
        order.setOrderStatus(OrderStatus.DELIVERED);
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(OrderStatus.PENDING);
        when(orderRepository.findByIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, updateRequest))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void cancelOrder_ShouldCancelOrderSuccessfully() {
        // Given
        when(orderRepository.findByIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        OrderDTO result = orderService.cancelOrder(1L);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenOrderShipped() {
        // Given
        order.setOrderStatus(OrderStatus.SHIPPED);
        when(orderRepository.findByIdAndTenantId(1L, "test-tenant")).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void getOrdersByStatus_ShouldReturnOrders() {
        // Given
        when(orderRepository.findByTenantIdAndOrderStatus("test-tenant", OrderStatus.PENDING))
                .thenReturn(List.of(order));

        // When
        List<OrderDTO> results = orderService.getOrdersByStatus(OrderStatus.PENDING);

        // Then
        assertThat(results).hasSize(1);
        verify(orderRepository).findByTenantIdAndOrderStatus("test-tenant", OrderStatus.PENDING);
    }

    @Test
    void getOrdersByCustomer_ShouldReturnOrders() {
        // Given
        when(orderRepository.findByTenantIdAndCustomerId("test-tenant", 1L))
                .thenReturn(List.of(order));

        // When
        List<OrderDTO> results = orderService.getOrdersByCustomer(1L);

        // Then
        assertThat(results).hasSize(1);
        verify(orderRepository).findByTenantIdAndCustomerId("test-tenant", 1L);
    }

    @Test
    void getAllOrders_ShouldReturnAllOrders() {
        // Given
        when(orderRepository.findByTenantId("test-tenant")).thenReturn(List.of(order));

        // When
        List<OrderDTO> results = orderService.getAllOrders();

        // Then
        assertThat(results).hasSize(1);
        verify(orderRepository).findByTenantId("test-tenant");
    }
}