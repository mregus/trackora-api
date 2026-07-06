package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FleetTelematicsLocationResponse(
        UUID vehicleId,
        String status,
        String label,
        String licensePlate,
        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        BigDecimal fuelLevelPercent,
        boolean checkEngine,
        Integer headingDegrees,
        Instant recordedAt
) {}