package com.fleetwise.api.telematics.geometris;

import java.math.BigDecimal;
import java.time.Instant;

public record GeometrisGpsTrailPoint(
        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        Instant recordedAt
) {}