package com.fleetwise.api.notification.dto;

import com.fleetwise.api.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {}