package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public interface FleetCopilotTool {

    String name();

    String description();

    Map<String, Object> parametersSchema();

    String execute(
            UUID fleetId,
            UUID userId,
            JsonNode arguments
    );
}