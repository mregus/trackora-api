package com.fleetwise.api.maintenance.dto;

import com.fleetwise.api.maintenance.entity.MaintenanceStatus;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record MaintenanceResponse(
        UUID id,
        UUID vehicleId,
        String serviceType,
        String description,
        LocalDate serviceDate,
        Integer mileage,
        BigDecimal cost,
        String vendor,
        LocalDate nextServiceDate,
        MaintenanceStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
