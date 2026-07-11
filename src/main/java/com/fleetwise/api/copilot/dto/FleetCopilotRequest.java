package com.fleetwise.api.copilot.dto;

import jakarta.validation.constraints.NotBlank;

public record FleetCopilotRequest(
        @NotBlank String question
) {
}