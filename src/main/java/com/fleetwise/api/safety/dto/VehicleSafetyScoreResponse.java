package com.fleetwise.api.safety.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleSafetyScoreResponse(
        UUID vehicleId,
        String label,
        String licensePlate,
        int score,
        int hardBrakes,
        int hardAccelerations,
        int harshTurns,
        int speedingEvents,
        int idleMinutes,
        boolean checkEngine,
        BigDecimal milesDriven
) {}