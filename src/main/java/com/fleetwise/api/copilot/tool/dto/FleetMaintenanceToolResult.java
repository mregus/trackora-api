package com.fleetwise.api.copilot.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FleetMaintenanceToolResult(
        UUID maintenanceId,
        UUID vehicleId,
        String vehicleName,
        String licensePlate,
        String serviceType,
        LocalDate serviceDate,
        String status,
        BigDecimal cost,
        boolean overdue,
        boolean dueSoon
) {
}