package com.fleetwise.api.search.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceSearchResult(
        UUID id,
        UUID vehicleId,
        UUID fleetId,
        String label,
        String serviceType,
        String status,
        LocalDate serviceDate
) {}