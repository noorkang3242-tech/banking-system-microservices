package com.bank.notification.dto;

import com.bank.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sent service-to-service (or by staff) to deliver a notification to a user.
 * The recipient is named explicitly (recipientUserId) — it is NOT the caller.
 */
public record SendNotificationRequest(

        @NotBlank(message = "recipientUserId is required")
        String recipientUserId,

        @NotNull(message = "type is required")
        NotificationType type,

        @NotBlank(message = "title is required")
        @Size(max = 150)
        String title,

        @NotBlank(message = "message is required")
        @Size(max = 500)
        String message
) {}
