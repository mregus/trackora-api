package com.fleetwise.api.telematics.dto;

import com.fleetwise.api.telematics.entity.TelematicsProvider;

import java.time.Instant;
import java.util.UUID;

public record TelematicsDeviceResponse(
        UUID id,
        UUID vehicleId,
        TelematicsProvider provider,
        String externalDeviceId,
        String serialNumber,
        String imei,
        String vin,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant lastSeenAt
) {}