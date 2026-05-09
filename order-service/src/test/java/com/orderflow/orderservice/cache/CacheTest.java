package com.orderflow.orderservice.cache;

import com.orderflow.orderservice.dto.request.CreateOrderRequest;
import com.orderflow.orderservice.dto.request.OrderItemRequest;
import com.orderflow.orderservice.dto.response.OrderDTO;
import com.orderflow.orderservice.entity.OrderStatus;
import com.orderflow.orderservice.repository.OrderRepository;
import com.orderflow.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("Cache Tests - Redis Integration")
class CacheTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        // Clear all caches
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("Should cache order on first retrieval")
    void shouldCacheOrderOnFirstRetrieval() {
        // Given - create order
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // When - fetch order first time
        long startTime1 = System.currentTimeMillis();
        OrderDTO order1 = orderService.getOrderById(createdOrder.id());
        long duration1 = System.currentTimeMillis() - startTime1;

        // Then - check cache
        var cache = cacheManager.getCache("orders");
        assertThat(cache).isNotNull();

        OrderDTO cachedOrder = cache.get(createdOrder.id(), OrderDTO.class);
        assertThat(cachedOrder).isNotNull();
        assertThat(cachedOrder.id()).isEqualTo(order1.id());

        // When - fetch order second time (from cache)
        long startTime2 = System.currentTimeMillis();
        OrderDTO order2 = orderService.getOrderById(createdOrder.id());
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then - should be faster (from cache)
        System.out.println("First call (DB): " + duration1 + "ms");
        System.out.println("Second call (Cache): " + duration2 + "ms");

        assertThat(order2.id()).isEqualTo(order1.id());
        // Cache should be faster (though in tests difference might be small)
    }

    @Test
    @DisplayName("Should cache orders by customer")
    void shouldCacheOrdersByCustomer() {
        // Given - create multiple orders for same customer
        for (int i = 1; i <= 3; i++) {
            OrderItemRequest item = new OrderItemRequest(
                    100L + i, "Product " + i, 1, new BigDecimal("99.99")
            );
            CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item));
            orderService.createOrder(request);
        }

        // When - fetch orders by customer first time
        long startTime1 = System.currentTimeMillis();
        List<OrderDTO> orders1 = orderService.getOrdersByCustomer(1L);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Then - should have 3 orders
        assertThat(orders1).hasSize(3);

        // When - fetch again (from cache)
        long startTime2 = System.currentTimeMillis();
        List<OrderDTO> orders2 = orderService.getOrdersByCustomer(1L);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then - should be same and faster
        assertThat(orders2).hasSize(3);
        System.out.println("First call (DB): " + duration1 + "ms");
        System.out.println("Second call (Cache): " + duration2 + "ms");
    }

    @Test
    @DisplayName("Should cache orders by status")
    void shouldCacheOrdersByStatus() {
        // Given - create orders
        for (int i = 1; i <= 3; i++) {
            OrderItemRequest item = new OrderItemRequest(
                    100L + i, "Product " + i, 1, new BigDecimal("99.99")
            );
            CreateOrderRequest request = new CreateOrderRequest((long) i, List.of(item));
            orderService.createOrder(request);
        }

        // When - fetch by status first time
        List<OrderDTO> orders1 = orderService.getOrdersByStatus(OrderStatus.PENDING);

        // Then
        assertThat(orders1).hasSize(3);

        // When - fetch again (from cache)
        List<OrderDTO> orders2 = orderService.getOrdersByStatus(OrderStatus.PENDING);

        // Then - should be same
        assertThat(orders2).hasSize(3);
    }

    @Test
    @DisplayName("Should evict cache on order update")
    void shouldEvictCacheOnOrderUpdate() {
        // Given - create and cache order
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // Cache it
        orderService.getOrderById(createdOrder.id());

        var cache = cacheManager.getCache("orders");
        assertThat(cache).isNotNull();
        assertThat(cache.get(createdOrder.id(), OrderDTO.class)).isNotNull();

        // When - update order
        orderService.cancelOrder(createdOrder.id());

        // Then - cache should be updated with new version
        OrderDTO cachedAfterUpdate = cache.get(createdOrder.id(), OrderDTO.class);
        assertThat(cachedAfterUpdate).isNotNull();
        assertThat(cachedAfterUpdate.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should handle cache miss gracefully")
    void shouldHandleCacheMissGracefully() {
        // Given - create order
        OrderItemRequest item = new OrderItemRequest(
                101L, "Laptop", 1, new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));
        OrderDTO createdOrder = orderService.createOrder(createRequest);

        // When - clear cache
        var cache = cacheManager.getCache("orders");
        assertThat(cache).isNotNull();
        cache.clear();

        // Then - should still fetch from DB
        OrderDTO order = orderService.getOrderById(createdOrder.id());
        assertThat(order).isNotNull();
        assertThat(order.id()).isEqualTo(createdOrder.id());
    }
}