package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TripResponse(
        Instant startTime,
        Instant endTime,
        long durationMinutes,
        BigDecimal distanceMiles,
        BigDecimal avgSpeedMph,
        BigDecimal maxSpeedMph,
        int pointCount
) {
}