package com.orderflow.notificationservice.service;

import com.orderflow.notificationservice.entity.Notification;
import com.orderflow.notificationservice.entity.NotificationStatus;
import com.orderflow.notificationservice.entity.NotificationType;
import com.orderflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification sendNotification(String tenantId, Long orderId, Long customerId,
                                         NotificationType type, String subject, String message) {

        log.info("Sending {} notification for order: {} to customer: {}",
                type, orderId, customerId);

        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .orderId(orderId)
                .customerId(customerId)
                .notificationType(type)
                .channel("EMAIL")
                .recipient("customer" + customerId + "@example.com")
                .subject(subject)
                .message(message)
                .status(NotificationStatus.PENDING)
                .build();

        try {
            // Simulate email sending
            simulateEmailSending(notification);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            log.info("Notification sent successfully: {}", notification.getId());

        } catch (Exception e) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());

            log.error("Failed to send notification: {}", e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    private void simulateEmailSending(Notification notification) {
        // Simulate email sending delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Email sent to: {} - Subject: {}",
                notification.getRecipient(), notification.getSubject());
    }

    public List<Notification> getNotificationsByOrder(String tenantId, Long orderId) {
        return notificationRepository.findByTenantIdAndOrderId(tenantId, orderId);
    }

    public List<Notification> getNotificationsByCustomer(String tenantId, Long customerId) {
        return notificationRepository.findByTenantIdAndCustomerId(tenantId, customerId);
    }
}