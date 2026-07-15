package com.fleetwise.api.copilot.ai.dto;

import java.util.Map;

public record OpenAiFunctionTool(
        String type,
        String name,
        String description,
        Map<String, Object> parameters,
        boolean strict
) {
}