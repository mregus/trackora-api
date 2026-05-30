package com.fleetwise.api.search.dto;

import java.util.UUID;

public record VehicleSearchResult(
        UUID id,
        UUID fleetId,
        String label,
        String vin,
        String licensePlate,
        Integer year,
        String make,
        String model
) {}