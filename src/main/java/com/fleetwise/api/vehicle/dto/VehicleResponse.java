package com.fleetwise.api.vehicle.dto;

import com.fleetwise.api.vehicle.entity.VehicleStatus;

import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        UUID fleetId,
        String vin,
        String make,
        String model,
        Integer year,
        String licensePlate,
        Integer currentMileage,
        VehicleStatus status,
        Instant createdAt,
        Instant updatedAt
) {}