package com.fleetwise.api.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fleetwise.api.copilot.ai.dto.OpenAiFunctionTool;

import java.util.List;

public record OpenAiToolRequest(
        String model,
        String instructions,
        Object input,
        List<OpenAiFunctionTool> tools,

        @JsonProperty("tool_choice")
        String toolChoice
) {
}