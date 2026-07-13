package com.fleetwise.api.copilot.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record FleetCopilotRequest(
        @NotBlank String question,
        UUID conversationId
) {
}