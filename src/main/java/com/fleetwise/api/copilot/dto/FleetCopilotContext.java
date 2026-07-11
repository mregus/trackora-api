package com.fleetwise.api.copilot.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FleetCopilotContext(
        UUID fleetId,
        String fleetName,

        long totalVehicles,
        long activeVehicles,
        long onlineVehicles,
        long staleVehicles,
        long offlineVehicles,

        long openAlerts,
        long criticalAlerts,
        long warningAlerts,

        long overdueMaintenance,
        long maintenanceDueSoon,

        BigDecimal monthlyMaintenanceCost,
        BigDecimal monthlyFuelCost,

        double averageSafetyScore,
        List<VehicleRiskSummary> highestRiskVehicles
) {
}