package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.service.DashboardService;
import com.fleetwise.api.fleet.service.FleetAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetFleetSummaryTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_fleet_summary";
    }

    @Override
    public String description() {
        return """
                Returns current fleet totals, online and offline vehicle counts,
                alerts, maintenance, fuel costs, safety status, packets, and trips.
                """;
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "additionalProperties", false
        );
    }

    @Override
    public String execute(
            UUID fleetId,
            UUID userId,
            JsonNode arguments
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        DashboardSummaryResponse summary =
                dashboardService.getSystemSummary(fleetId);

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize fleet summary",
                    ex
            );
        }
    }
}