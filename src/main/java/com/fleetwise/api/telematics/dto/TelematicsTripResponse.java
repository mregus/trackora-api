package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TelematicsTripResponse(
        Instant startTime,
        Instant endTime,
        int pointCount,
        BigDecimal maxSpeedMph,
        BigDecimal avgSpeedMph,
        Long durationMinutes,
        BigDecimal distanceMiles
) {}