package com.orderflow.inventoryservice.controller;

import com.orderflow.inventoryservice.dto.ReserveStockRequest;
import com.orderflow.inventoryservice.dto.ReserveStockResponse;
import com.orderflow.inventoryservice.dto.StockStatusResponse;
import com.orderflow.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    public ResponseEntity<ReserveStockResponse> reserveStock(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody ReserveStockRequest request
    ) {
        log.info("REST: Reserve stock - Tenant: {}, Product: {}", tenantId, request.productId());
        ReserveStockResponse response = inventoryService.reserveStock(tenantId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release/{reservationId}")
    public ResponseEntity<String> releaseReservation(@PathVariable String reservationId) {
        log.info("REST: Release reservation: {}", reservationId);
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.ok("Reservation released successfully");
    }

    @PostMapping("/confirm/{reservationId}")
    public ResponseEntity<String> confirmReservation(@PathVariable String reservationId) {
        log.info("REST: Confirm reservation: {}", reservationId);
        inventoryService.confirmReservation(reservationId);
        return ResponseEntity.ok("Reservation confirmed successfully");
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<StockStatusResponse> getStockStatus(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable Long productId
    ) {
        log.info("REST: Get stock status - Product: {}", productId);
        StockStatusResponse response = inventoryService.getStockStatus(tenantId, productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeProducts(
            @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        log.info("REST: Initialize sample products");
        inventoryService.initializeSampleProducts(tenantId);
        return ResponseEntity.ok("Sample products initialized");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory Service is running");
    }
}