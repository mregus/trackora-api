package com.fleetwise.api.copilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CopilotConversationDetailResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<CopilotMessageResponse> messages
) {
}