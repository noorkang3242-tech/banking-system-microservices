package com.bank.notification.controller;

import com.bank.notification.dto.NotificationResponse;
import com.bank.notification.dto.SendNotificationRequest;
import com.bank.notification.dto.UnreadCountResponse;
import com.bank.notification.exception.ApiException;
import com.bank.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications/alerts")
public class NotificationController {

    private final NotificationService notificationService;

    private static final Set<String> STAFF_ROLES = Set.of("MANAGER", "ADMIN");

    /** Called service-to-service (or by staff) to deliver a notification to a user. */
    @PostMapping("/send")
    @Operation(summary = "Send a notification to a user (staff/internal only)")
    public ResponseEntity<NotificationResponse> send(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody SendNotificationRequest request) {
        requireStaff(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.send(request));
    }

    @GetMapping("/me")
    @Operation(summary = "My notifications (newest first)")
    public ResponseEntity<List<NotificationResponse>> mine(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(notificationService.getMine(userId));
    }

    @GetMapping("/me/unread")
    @Operation(summary = "My unread notifications")
    public ResponseEntity<List<NotificationResponse>> myUnread(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(notificationService.getMyUnread(userId));
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "How many unread notifications I have")
    public ResponseEntity<UnreadCountResponse> unreadCount(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.unreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<NotificationResponse> markRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, userId));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<Map<String, Object>> markAllRead(@RequestHeader("X-User-Id") String userId) {
        long updated = notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("markedRead", updated));
    }

    @GetMapping
    @Operation(summary = "List all notifications (staff only)")
    public ResponseEntity<List<NotificationResponse>> getAll(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireStaff(role);
        return ResponseEntity.ok(notificationService.getAll());
    }

    private void requireStaff(String role) {
        if (role == null || !STAFF_ROLES.contains(role.toUpperCase())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requires MANAGER or ADMIN role");
        }
    }
}
