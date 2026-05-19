package com.fleetwise.api.vehicle.dto;

import com.fleetwise.api.vehicle.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record UpdateVehicleRequest(

        @Schema(example = "1FADP3L90GL123456")
        @Size(max = 50)
        String vin,

        @Schema(example = "Ford")
        @NotBlank
        @Size(max = 100)
        String make,

        @Schema(example = "Focus ST")
        @NotBlank
        @Size(max = 100)
        String model,

        @Schema(example = "2016")
        @NotNull
        @Min(1900)
        @Max(2100)
        Integer year,

        @Schema(example = "NEW123")
        @Size(max = 50)
        String licensePlate,

        @Schema(example = "110000")
        @Min(0)
        Integer currentMileage,

        @Schema(example = "ACTIVE")
        @NotNull
        VehicleStatus status,

        UUID fleetId
) {}