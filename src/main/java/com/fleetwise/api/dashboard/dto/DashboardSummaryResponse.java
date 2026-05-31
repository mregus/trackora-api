package com.fleetwise.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record DashboardSummaryResponse(
        UUID fleetId,
        String fleetName,
        long totalVehicles,
        long activeVehicles,
        long vehiclesInShop,
        long outOfServiceVehicles,
        long openAlerts,
        BigDecimal monthlyMaintenanceCost,
        BigDecimal monthlyFuelCost,
        String latestAiInsight,
        Integer fleetHealthScore,
        FleetHealthBreakdown fleetHealthBreakdown
) {}