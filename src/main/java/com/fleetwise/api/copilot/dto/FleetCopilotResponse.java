package com.fleetwise.api.copilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FleetCopilotResponse(
        UUID conversationId,
        String answer,
        List<String> supportingFacts,
        Instant generatedAt,
        boolean aiGenerated
) {
}