package com.orderflow.notificationservice.repository;

import com.orderflow.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTenantIdAndOrderId(String tenantId, Long orderId);
    List<Notification> findByTenantIdAndCustomerId(String tenantId, Long customerId);
}