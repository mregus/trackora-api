package com.fleetwise.api.copilot.tool.dto;

import java.time.Instant;
import java.util.UUID;

public record FleetAlertToolResult(
        UUID alertId,
        UUID vehicleId,
        String vehicleName,
        String licensePlate,
        String type,
        String severity,
        String message,
        boolean resolved,
        Instant createdAt
) {
}