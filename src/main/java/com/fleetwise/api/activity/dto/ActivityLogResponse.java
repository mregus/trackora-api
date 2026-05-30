package com.fleetwise.api.activity.dto;

import com.fleetwise.api.activity.entity.ActivityAction;

import java.time.Instant;
import java.util.UUID;

public record ActivityLogResponse(
        UUID id,
        UUID fleetId,
        UUID vehicleId,
        UUID userId,
        ActivityAction action,
        String entityType,
        UUID entityId,
        String message,
        Instant createdAt
) {}