package com.fleetwise.api.copilot.conversation.dto;

import java.time.Instant;
import java.util.UUID;

public record CopilotConversationSummaryResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}