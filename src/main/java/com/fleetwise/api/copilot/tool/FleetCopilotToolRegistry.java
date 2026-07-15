package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FleetCopilotToolRegistry {

    private final Map<String, FleetCopilotTool> tools;

    public FleetCopilotToolRegistry(List<FleetCopilotTool> tools) {
        this.tools = tools.stream()
                .collect(Collectors.toUnmodifiableMap(
                        FleetCopilotTool::name,
                        Function.identity()
                ));

        log.info(
                "Registered Fleet Copilot tools: {}",
                this.tools.keySet()
        );
    }

    public List<FleetCopilotTool> all() {
        return List.copyOf(tools.values());
    }

    public String execute(
            String toolName,
            UUID fleetId,
            UUID userId,
            JsonNode arguments
    ) {
        FleetCopilotTool tool = tools.get(toolName);

        if (tool == null) {
            throw new IllegalArgumentException(
                    "Unsupported Copilot tool: " + toolName
            );
        }

        return tool.execute(fleetId, userId, arguments);
    }
}