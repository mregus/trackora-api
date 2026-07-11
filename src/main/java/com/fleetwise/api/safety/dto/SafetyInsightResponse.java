package com.fleetwise.api.safety.dto;

import java.time.Instant;
import java.util.List;

public record SafetyInsightResponse(
        String summary,
        List<String> recommendations,
        Instant generatedAt
) {
}