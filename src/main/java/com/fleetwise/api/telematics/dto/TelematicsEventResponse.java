package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelematicsEventResponse(
        UUID id,
        UUID vehicleId,
        Instant recordedAt,

        Double latitude,
        Double longitude,

        BigDecimal speedMph,
        BigDecimal odometerMiles,
        BigDecimal fuelLevelPercent,
        BigDecimal engineTempF,
        BigDecimal batteryVoltage,

        boolean checkEngine,
        boolean harshBraking,
        Integer idleMinutes
) {}