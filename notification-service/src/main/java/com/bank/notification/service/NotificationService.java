package com.bank.notification.service;

import com.bank.notification.dto.NotificationResponse;
import com.bank.notification.dto.SendNotificationRequest;
import com.bank.notification.entity.Notification;
import com.bank.notification.entity.NotificationStatus;
import com.bank.notification.exception.ApiException;
import com.bank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final SendRateLimiter rateLimiter;

    public NotificationResponse send(SendNotificationRequest req) {
        rateLimiter.check(req.recipientUserId());
        Notification n = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(req.recipientUserId())
                .type(req.type())
                .title(req.title())
                .message(req.message())
                .status(NotificationStatus.UNREAD)
                .build();
        return NotificationResponse.from(repository.save(n));
    }

    public List<NotificationResponse> getMine(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from).toList();
    }

    public List<NotificationResponse> getMyUnread(String userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD).stream()
                .map(NotificationResponse::from).toList();
    }

    public long unreadCount(String userId) {
        return repository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markRead(String notificationId, String userId) {
        Notification n = repository.findByNotificationId(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This notification is not yours");
        }
        if (n.getStatus() != NotificationStatus.READ) {
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(Instant.now());
            repository.save(n);
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public long markAllRead(String userId) {
        List<Notification> unread =
                repository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
        Instant now = Instant.now();
        unread.forEach(n -> {
            n.setStatus(NotificationStatus.READ);
            n.setReadAt(now);
        });
        repository.saveAll(unread);
        return unread.size();
    }

    public List<NotificationResponse> getAll() {
        return repository.findAll().stream().map(NotificationResponse::from).toList();
    }
}
