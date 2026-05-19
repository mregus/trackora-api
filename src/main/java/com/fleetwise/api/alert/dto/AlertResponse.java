package com.fleetwise.api.alert.dto;

import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID fleetId,
        UUID vehicleId,
        AlertType type,
        String message,
        boolean resolved,
        AlertSeverity severity,
        Instant createdAt,
        Instant resolvedAt
) {}
