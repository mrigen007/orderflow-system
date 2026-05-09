package com.orderflow.orderservice.concurrency;

import com.orderflow.orderservice.dto.request.CreateOrderRequest;
import com.orderflow.orderservice.dto.request.OrderItemRequest;
import com.orderflow.orderservice.dto.request.UpdateOrderRequest;
import com.orderflow.orderservice.dto.response.OrderDTO;
import com.orderflow.orderservice.entity.Order;
import com.orderflow.orderservice.entity.OrderStatus;
import com.orderflow.orderservice.repository.OrderRepository;
import com.orderflow.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("Concurrency Tests - Optimistic & Pessimistic Locking")
class ConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Should handle concurrent updates with optimistic locking")
    void shouldHandleConcurrentUpdatesWithOptimisticLocking() throws Exception {
        // Given - create an order
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // When - 10 threads try to update simultaneously
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Try to update to CONFIRMED
                    UpdateOrderRequest updateRequest = new UpdateOrderRequest(OrderStatus.CONFIRMED);
                    orderService.updateOrderStatus(createdOrder.id(), updateRequest);

                    successCount.incrementAndGet();

                } catch (ObjectOptimisticLockingFailureException ex) {
                    // Expected for some threads
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        completionLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - verify results
        System.out.println("Successes: " + successCount.get());
        System.out.println("Failures: " + failureCount.get());

        // Only one should succeed, others should fail with OptimisticLockException
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(numberOfThreads - 1);

        // Verify final state
        Order finalOrder = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(finalOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(finalOrder.getVersion()).isEqualTo(1L); // Version incremented once
    }

    @Test
    @DisplayName("Should prevent lost updates with version field")
    void shouldPreventLostUpdatesWithVersionField() throws Exception {
        // Given - create order
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // When - Simulate lost update scenario
        // Thread A reads order (version=0)
        Order orderThreadA = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(orderThreadA.getVersion()).isEqualTo(0L);

        // Thread B reads order (version=0)
        Order orderThreadB = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(orderThreadB.getVersion()).isEqualTo(0L);

        // Thread A updates successfully
        orderThreadA.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(orderThreadA);

        Order afterThreadA = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(afterThreadA.getVersion()).isEqualTo(1L);
        assertThat(afterThreadA.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Thread B tries to update (still has version=0)
        orderThreadB.setOrderStatus(OrderStatus.CANCELLED);

        // Then - should throw OptimisticLockException
        try {
            orderRepository.save(orderThreadB);
            throw new AssertionError("Should have thrown OptimisticLockException");
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Expected - version mismatch detected
            System.out.println(" Optimistic lock prevented lost update!");
        }

        // Verify final state - Thread A's update preserved
        Order finalOrder = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(finalOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(finalOrder.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle pessimistic locking correctly")
    void shouldHandlePessimisticLockingCorrectly() throws Exception {
        // Given - create order
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // When - Use pessimistic locking
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    // Each thread tries to update with pessimistic lock
                    UpdateOrderRequest updateRequest = new UpdateOrderRequest(OrderStatus.CONFIRMED);
                    orderService.updateOrderStatusWithPessimisticLock(
                            createdOrder.id(),
                            updateRequest
                    );
                    successCount.incrementAndGet();
                    System.out.println("Thread " + threadNum + " succeeded");
                } catch (Exception ex) {
                    System.out.println("Thread " + threadNum + " failed: " + ex.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        completionLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - All threads should eventually succeed (they wait for lock)
        // In reality, they execute sequentially due to database lock
        System.out.println("Pessimistic lock successes: " + successCount.get());

        // Verify final state
        Order finalOrder = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(finalOrder.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should demonstrate difference between optimistic and pessimistic locking")
    void shouldDemonstrateLockingDifference() {
        // Given
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO order = orderService.createOrder(createRequest);

        System.out.println("\n=== OPTIMISTIC LOCKING ===");
        System.out.println(" Allows concurrent reads");
        System.out.println(" Detects conflicts on write");
        System.out.println(" Better performance for low-conflict scenarios");
        System.out.println(" Requires retry logic");
        System.out.println("Use case: Most order updates (conflicts rare)");

        System.out.println("\n=== PESSIMISTIC LOCKING ===");
        System.out.println(" Prevents conflicts entirely");
        System.out.println(" No retry needed");
        System.out.println(" Blocks other transactions");
        System.out.println(" Lower throughput");
        System.out.println("Use case: Critical financial operations");

        // Both achieve same result, different trade-offs
        assertThat(order).isNotNull();
    }

    @Test
    @DisplayName("Should handle race condition in total calculation")
    void shouldHandleRaceConditionInTotalCalculation() throws Exception {
        // Given - create order
        OrderItemRequest item1 = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1000.00")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item1));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // When - Multiple threads try to read and modify simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    UpdateOrderRequest updateRequest = new UpdateOrderRequest(OrderStatus.CONFIRMED);
                    orderService.updateOrderStatus(createdOrder.id(), updateRequest);
                } catch (Exception ignored) {
                    // Some will fail - that's expected
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - Verify data integrity maintained
        Order finalOrder = orderRepository.findById(createdOrder.id()).orElseThrow();
        assertThat(finalOrder.getTotalAmount()).isEqualTo(new BigDecimal("1000.00"));
        // Total amount should not be corrupted by concurrent access
    }
}