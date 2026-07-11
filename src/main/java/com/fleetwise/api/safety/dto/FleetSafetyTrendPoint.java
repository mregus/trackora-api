package com.fleetwise.api.safety.dto;

import java.time.LocalDate;

public record FleetSafetyTrendPoint(
        LocalDate date,
        double averageScore,
        int hardBrakes,
        int hardAccelerations,
        int harshTurns,
        int speedingEvents,
        int idleMinutes
) {}