package com.fleetwise.api.copilot.dto;

import java.util.UUID;

public record VehicleRiskSummary(
        UUID vehicleId,
        String vehicleName,
        String licensePlate,
        int safetyScore,
        int hardBrakes,
        int speedingEvents,
        int idleMinutes,
        boolean checkEngine
) {
}