package com.bank.notification.dto;

import com.bank.notification.entity.Notification;
import com.bank.notification.entity.NotificationStatus;
import com.bank.notification.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String notificationId,
        String userId,
        NotificationType type,
        String title,
        String message,
        NotificationStatus status,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
