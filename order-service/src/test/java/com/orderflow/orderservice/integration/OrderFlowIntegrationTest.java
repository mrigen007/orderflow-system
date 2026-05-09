package com.orderflow.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.orderservice.dto.request.CreateOrderRequest;
import com.orderflow.orderservice.dto.request.OrderItemRequest;
import com.orderflow.orderservice.dto.request.UpdateOrderRequest;
import com.orderflow.orderservice.dto.response.OrderDTO;
import com.orderflow.orderservice.entity.OrderStatus;
import com.orderflow.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Order Flow Integration Tests")
class OrderFlowIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create order successfully via REST API")
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        OrderItemRequest item = new OrderItemRequest(
                101L,
                "Laptop",
                1,
                new BigDecimal("1299.99")
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item));

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1299.99))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                .andReturn();

        // Verify database
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return 400 for invalid order request")
    void shouldReturn400ForInvalidOrderRequest() throws Exception {
        // Given - invalid customerId (negative)
        OrderItemRequest item = new OrderItemRequest(
                101L,
                "Laptop",
                1,
                new BigDecimal("1299.99")
        );
        CreateOrderRequest request = new CreateOrderRequest(-1L, List.of(item));

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.customerId").exists());
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void shouldGetOrderByIdSuccessfully() throws Exception {
        // Given - create order first
        OrderItemRequest item = new OrderItemRequest(
                101L,
                "Laptop",
                1,
                new BigDecimal("1299.99")
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item));

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        OrderDTO createdOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                OrderDTO.class
        );

        // When & Then
        mockMvc.perform(get("/api/v1/orders/" + createdOrder.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdOrder.id()))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent order")
    void shouldReturn404ForNonExistentOrder() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    @Test
    @DisplayName("Should complete full order lifecycle")
    void shouldCompleteFullOrderLifecycle() throws Exception {
        // Step 1: Create order
        OrderItemRequest item = new OrderItemRequest(
                101L,
                "Laptop",
                1,
                new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderDTO order = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                OrderDTO.class
        );

        // Step 2: Update to CONFIRMED
        UpdateOrderRequest confirmedRequest = new UpdateOrderRequest(OrderStatus.CONFIRMED);
        mockMvc.perform(put("/api/v1/orders/" + order.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));

        // Step 3: Update to PROCESSING
        UpdateOrderRequest processingRequest = new UpdateOrderRequest(OrderStatus.PROCESSING);
        mockMvc.perform(put("/api/v1/orders/" + order.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(processingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PROCESSING"));

        // Step 4: Update to SHIPPED
        UpdateOrderRequest shippedRequest = new UpdateOrderRequest(OrderStatus.SHIPPED);
        mockMvc.perform(put("/api/v1/orders/" + order.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shippedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("SHIPPED"));

        // Step 5: Update to DELIVERED
        UpdateOrderRequest deliveredRequest = new UpdateOrderRequest(OrderStatus.DELIVERED);
        mockMvc.perform(put("/api/v1/orders/" + order.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveredRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("DELIVERED"));
    }

    @Test
    @DisplayName("Should reject invalid status transition")
    void shouldRejectInvalidStatusTransition() throws Exception {
        // Given - create order
        OrderItemRequest item = new OrderItemRequest(
                101L,
                "Laptop",
                1,
                new BigDecimal("1299.99")
        );
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, List.of(item));

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        OrderDTO order = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                OrderDTO.class
        );

        // When & Then - try invalid transition PENDING -> SHIPPED
        UpdateOrderRequest shippedRequest = new UpdateOrderRequest(OrderStatus.SHIPPED);
        mockMvc.perform(put("/api/v1/orders/" + order.id() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shippedRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid status transition from PENDING to SHIPPED"));
    }

    @Test
    @DisplayName("Should cancel order successfully")
    void shouldCancelOrderSuccessfully() throws Exception {
        // Given - create order
        OrderItemRequest item = new OrderItemRequest(
                101L,
                "Laptop",
                1,
                new BigDecimal("1299.99")
        );
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item));

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        OrderDTO order = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                OrderDTO.class
        );

        // When & Then - cancel order
        mockMvc.perform(post("/api/v1/orders/" + order.id() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should get orders by status")
    void shouldGetOrdersByStatus() throws Exception {
        // Given - create multiple orders
        for (int i = 1; i <= 3; i++) {
            OrderItemRequest item = new OrderItemRequest(
                    100L + i,
                    "Product " + i,
                    1,
                    new BigDecimal("99.99")
            );
            CreateOrderRequest request = new CreateOrderRequest((long) i, List.of(item));

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // When & Then - get all PENDING orders
        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].orderStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Should get orders by customer")
    void shouldGetOrdersByCustomer() throws Exception {
        // Given - create orders for customer 1
        for (int i = 1; i <= 2; i++) {
            OrderItemRequest item = new OrderItemRequest(
                    100L + i,
                    "Product " + i,
                    1,
                    new BigDecimal("99.99")
            );
            CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item));

            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // Create order for customer 2
        OrderItemRequest item = new OrderItemRequest(
                103L,
                "Product 3",
                1,
                new BigDecimal("99.99")
        );
        CreateOrderRequest request = new CreateOrderRequest(2L, List.of(item));
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then - get orders for customer 1
        mockMvc.perform(get("/api/v1/orders")
                        .param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value(1));
    }
}