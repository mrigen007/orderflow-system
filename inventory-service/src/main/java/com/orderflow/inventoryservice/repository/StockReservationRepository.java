package com.orderflow.inventoryservice.repository;

import com.orderflow.inventoryservice.entity.StockReservation;
import com.orderflow.inventoryservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByReservationId(String reservationId);

    List<StockReservation> findByTenantIdAndOrderId(String tenantId, Long orderId);

    List<StockReservation> findByStatus(ReservationStatus status);
}