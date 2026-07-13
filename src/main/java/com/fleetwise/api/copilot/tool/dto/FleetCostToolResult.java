package com.fleetwise.api.copilot.tool.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FleetCostToolResult(
        UUID fleetId,
        String fleetName,
        BigDecimal fuelCost,
        BigDecimal maintenanceCost,
        BigDecimal totalRecordedCost,
        long totalVehicles
) {
}