package com.fleetwise.api.copilot.dto;

import com.fleetwise.api.copilot.entity.CopilotMessageRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CopilotMessageResponse(
        UUID id,
        CopilotMessageRole role,
        String content,
        List<String> supportingFacts,
        boolean aiGenerated,
        Instant createdAt
) {
}