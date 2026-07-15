package com.fleetwise.api.copilot.conversation.dto;

import com.fleetwise.api.copilot.dto.CopilotMessageResponse;

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