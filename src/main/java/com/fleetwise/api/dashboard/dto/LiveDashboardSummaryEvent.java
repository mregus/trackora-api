package com.fleetwise.api.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record LiveDashboardSummaryEvent(
        UUID fleetId,
        long onlineVehicles,
        long staleVehicles,
        long offlineVehicles,
        long openAlerts,
        long packetsToday,
        long tripsToday,
        Instant updatedAt
) {}