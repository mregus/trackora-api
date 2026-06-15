package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TelematicsHistoryPointResponse(
        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        Instant recordedAt
) {}
