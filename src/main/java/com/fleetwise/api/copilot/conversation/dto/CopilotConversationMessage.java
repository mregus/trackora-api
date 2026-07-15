package com.fleetwise.api.copilot.conversation.dto;

import com.fleetwise.api.copilot.entity.CopilotMessageRole;

import java.time.Instant;

public record CopilotConversationMessage(
        CopilotMessageRole role,
        String content,
        Instant createdAt
) {
}