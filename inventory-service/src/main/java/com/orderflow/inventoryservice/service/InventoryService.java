package com.orderflow.inventoryservice.service;

import com.orderflow.inventoryservice.dto.ReserveStockRequest;
import com.orderflow.inventoryservice.dto.ReserveStockResponse;
import com.orderflow.inventoryservice.dto.StockStatusResponse;
import com.orderflow.inventoryservice.entity.Product;
import com.orderflow.inventoryservice.entity.ReservationStatus;
import com.orderflow.inventoryservice.entity.StockReservation;
import com.orderflow.inventoryservice.exception.InsufficientStockException;
import com.orderflow.inventoryservice.exception.ProductNotFoundException;
import com.orderflow.inventoryservice.exception.ReservationNotFoundException;
import com.orderflow.inventoryservice.repository.ProductRepository;
import com.orderflow.inventoryservice.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockReservationRepository reservationRepository;

    @Transactional
    public ReserveStockResponse reserveStock(String tenantId, ReserveStockRequest request) {
        log.info("📦 Reserving stock - Tenant: {}, Product: {}, Quantity: {}, Order: {}",
                tenantId, request.productId(), request.quantity(), request.orderId());

        Product product = productRepository.findByTenantIdAndProductIdWithLock(tenantId, request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        if (product.getAvailableStock() < request.quantity()) {
            throw new InsufficientStockException(
                    request.productId(),
                    request.quantity(),
                    product.getAvailableStock()
            );
        }

        product.reserveStock(request.quantity());
        productRepository.save(product);

        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        StockReservation reservation = StockReservation.builder()
                .reservationId(reservationId)
                .tenantId(tenantId)
                .orderId(request.orderId())
                .productId(request.productId())
                .quantity(request.quantity())
                .status(ReservationStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        reservationRepository.save(reservation);

        log.info("✅ Stock reserved - Reservation ID: {}, Product: {}, Quantity: {}",
                reservationId, request.productId(), request.quantity());

        return new ReserveStockResponse(
                reservationId,
                request.productId(),
                request.quantity(),
                "RESERVED",
                "Stock successfully reserved"
        );
    }

    @Transactional
    public void releaseReservation(String reservationId) {
        log.info("🔓 Releasing reservation: {}", reservationId);

        StockReservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            log.warn("⚠️ Reservation {} is not active (status: {})", reservationId, reservation.getStatus());
            return;
        }

        Product product = productRepository.findByTenantIdAndProductIdWithLock(
                reservation.getTenantId(),
                reservation.getProductId()
        ).orElseThrow(() -> new ProductNotFoundException(reservation.getProductId()));

        product.releaseStock(reservation.getQuantity());
        productRepository.save(product);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        log.info("✅ Reservation released: {}, Quantity: {}", reservationId, reservation.getQuantity());
    }

    @Transactional
    public void confirmReservation(String reservationId) {
        log.info("✅ Confirming reservation: {}", reservationId);

        StockReservation reservation = reservationRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            log.warn("⚠️ Reservation {} is not active", reservationId);
            return;
        }

        Product product = productRepository.findByTenantIdAndProductIdWithLock(
                reservation.getTenantId(),
                reservation.getProductId()
        ).orElseThrow(() -> new ProductNotFoundException(reservation.getProductId()));

        product.confirmReservation(reservation.getQuantity());
        productRepository.save(product);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        log.info("✅ Reservation confirmed: {}", reservationId);
    }

    public StockStatusResponse getStockStatus(String tenantId, Long productId) {
        log.info("📊 Getting stock status - Tenant: {}, Product: {}", tenantId, productId);

        Product product = productRepository.findByTenantIdAndProductId(tenantId, productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return new StockStatusResponse(
                product.getProductId(),
                product.getName(),
                product.getTotalStock(),
                product.getReservedStock(),
                product.getAvailableStock()
        );
    }

    @Transactional
    public void initializeSampleProducts(String tenantId) {
        log.info("🌱 Initializing sample products for tenant: {}", tenantId);

        if (productRepository.findByTenantIdAndProductId(tenantId, 101L).isEmpty()) {
            Product laptop = Product.builder()
                    .tenantId(tenantId)
                    .productId(101L)
                    .name("Laptop")
                    .totalStock(100)
                    .reservedStock(0)
                    .availableStock(100)
                    .build();
            productRepository.save(laptop);
        }

        if (productRepository.findByTenantIdAndProductId(tenantId, 102L).isEmpty()) {
            Product mouse = Product.builder()
                    .tenantId(tenantId)
                    .productId(102L)
                    .name("Mouse")
                    .totalStock(500)
                    .reservedStock(0)
                    .availableStock(500)
                    .build();
            productRepository.save(mouse);
        }

        if (productRepository.findByTenantIdAndProductId(tenantId, 103L).isEmpty()) {
            Product keyboard = Product.builder()
                    .tenantId(tenantId)
                    .productId(103L)
                    .name("Keyboard")
                    .totalStock(300)
                    .reservedStock(0)
                    .availableStock(300)
                    .build();
            productRepository.save(keyboard);
        }

        log.info("✅ Sample products initialized");
    }
}