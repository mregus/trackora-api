package com.fleetwise.api.copilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameCopilotConversationRequest(
        @NotBlank
        @Size(max = 200)
        String title
) {
}