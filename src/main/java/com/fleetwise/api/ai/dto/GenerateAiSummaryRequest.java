package com.fleetwise.api.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GenerateAiSummaryRequest(

        @Schema(example = "last 30 days")
        String timeframe,
        @Schema(example = "true")
        boolean includeFuelStats,
        @Schema(example = "true")
        boolean includeMaintenanceStats
) {}
