package com.bank.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification", indexes = {
        // cover the user_id-only list (ORDER BY created_at) and the user_id+status list/count
        @Index(name = "idx_notif_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_notif_user_status_created", columnList = "user_id,status,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", unique = true, nullable = false, length = 36)
    private String notificationId;

    /** Recipient's auth-service user_id. */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = NotificationStatus.UNREAD;
    }
}
