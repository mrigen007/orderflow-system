package com.orderflow.notificationservice.controller;

import com.orderflow.notificationservice.entity.Notification;
import com.orderflow.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getNotificationsByOrder(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable Long orderId
    ) {
        log.info("Fetching notifications for order: {} tenant: {}", orderId, tenantId);
        List<Notification> notifications = notificationService.getNotificationsByOrder(tenantId, orderId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Notification>> getNotificationsByCustomer(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable Long customerId
    ) {
        log.info("Fetching notifications for customer: {} tenant: {}", customerId, tenantId);
        List<Notification> notifications = notificationService.getNotificationsByCustomer(tenantId, customerId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}