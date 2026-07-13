package com.fleetwise.api.copilot.tool.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VehicleDetailsToolResult(
        UUID vehicleId,
        String vehicleName,
        String licensePlate,
        String vin,
        String status,

        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        BigDecimal fuelLevelPercent,
        Integer headingDegrees,
        boolean checkEngine,
        Instant lastSeenAt,
        String deviceStatus,

        Integer safetyScore,
        Integer hardBrakes,
        Integer hardAccelerations,
        Integer harshTurns,
        Integer speedingEvents,
        Integer idleMinutes
) {
}