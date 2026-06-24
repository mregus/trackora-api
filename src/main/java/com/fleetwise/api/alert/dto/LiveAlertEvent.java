package com.fleetwise.api.alert.dto;

import java.time.Instant;
import java.util.UUID;

public record LiveAlertEvent(
        UUID alertId,
        UUID fleetId,
        UUID vehicleId,
        String vehicleName,
        String licensePlate,
        String type,
        String severity,
        String message,
        Instant createdAt
) {}