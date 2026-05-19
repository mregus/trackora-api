package com.fleetwise.api.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record AiInsightResponse(
        UUID id,
        UUID fleetId,
        String summary,
        Instant createdAt
) {}
