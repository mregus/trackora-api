package com.fleetwise.api.dashboard.dto;

public record FleetHealthBreakdown(
        int criticalAlerts,
        int warningAlerts,
        int overdueMaintenance,
        int maintenanceDueSoon
) {}