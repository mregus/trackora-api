package com.fleetwise.api.telematics.dto;

import com.fleetwise.api.telematics.entity.TelematicsProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterTelematicsDeviceRequest(
        @NotNull UUID vehicleId,

        @NotNull TelematicsProvider provider,

        @NotBlank String externalDeviceId,

        String serialNumber,
        String imei,
        String vin
) {}