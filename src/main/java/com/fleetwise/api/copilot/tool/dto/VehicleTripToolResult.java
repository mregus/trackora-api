package com.fleetwise.api.copilot.tool.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VehicleTripToolResult(
        UUID vehicleId,
        String vehicleName,
        String licensePlate,
        Instant startTime,
        Instant endTime,
        long durationMinutes,
        BigDecimal distanceMiles,
        BigDecimal averageSpeedMph,
        BigDecimal maxSpeedMph,
        int pointCount
) {
}