package com.fleetwise.api.telematics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LiveVehicleLocationEvent(
        UUID vehicleId,
        UUID fleetId,
        String vehicleName,
        String licensePlate,
        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        Integer headingDegrees,
        BigDecimal fuelLevelPercent,
        boolean checkEngine,
        Instant recordedAt
) {}