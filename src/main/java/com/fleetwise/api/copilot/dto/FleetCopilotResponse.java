package com.fleetwise.api.copilot.dto;

import java.time.Instant;
import java.util.List;

public record FleetCopilotResponse(
        String answer,
        List<String> supportingFacts,
        Instant generatedAt,
        boolean aiGenerated
) {
}