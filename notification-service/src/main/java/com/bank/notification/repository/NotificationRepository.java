package com.bank.notification.repository;

import com.bank.notification.entity.Notification;
import com.bank.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, NotificationStatus status);

    Optional<Notification> findByNotificationId(String notificationId);

    long countByUserIdAndStatus(String userId, NotificationStatus status);
}
