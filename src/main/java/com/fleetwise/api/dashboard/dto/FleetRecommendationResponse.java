package com.fleetwise.api.dashboard.dto;

public record FleetRecommendationResponse(
        String type,
        String message,
        String severity
) {}
