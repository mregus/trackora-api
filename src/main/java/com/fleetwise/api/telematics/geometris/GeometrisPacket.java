package com.fleetwise.api.telematics.geometris;

import java.math.BigDecimal;
import java.time.Instant;

public record GeometrisPacket(
        Boolean ignitionOn,
        String formatCrc,
        String serialNumber,
        String reasonText,
        Instant recordedAt,
        Double latitude,
        Double longitude,
        BigDecimal speedMph,
        Integer headingDegrees,
        BigDecimal gpsOdometerMiles,
        Integer ignitionDurationSeconds,
        Integer totalIdleDurationSeconds,
        Integer engineRpm,
        Integer coolantTempC,
        BigDecimal ecuOdometerMiles,
        String vin,
        BigDecimal fuelLevelPercent,
        String activeDtc,
        BigDecimal batteryVoltage,
        String locationTrail
) {}