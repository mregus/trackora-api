package com.fleetwise.api.fuel.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record FuelLogResponse(
        UUID id,
        UUID vehicleId,
        LocalDate fuelDate,
        Integer mileage,
        BigDecimal gallons,
        BigDecimal totalCost,
        BigDecimal pricePerGallon,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
