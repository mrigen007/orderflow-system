package com.orderflow.orderservice.repository;

import com.orderflow.orderservice.entity.Order;
import com.orderflow.orderservice.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByTenantIdAndCustomerId(String tenantId, Long customerId);

    List<Order> findByTenantIdAndOrderStatus(String tenantId, OrderStatus orderStatus);

    Optional<Order> findByIdAndTenantId(Long id, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.tenantId = :tenantId")
    Optional<Order> findByIdAndTenantIdWithLock(@Param("id") Long id, @Param("tenantId") String tenantId);

    List<Order> findByTenantId(String tenantId);
}