package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelematicsHistoryPointResponse(
        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        Instant recordedAt,

        UUID vehicleId,
        String make,
        String model,
        String licensePlate
) {}
