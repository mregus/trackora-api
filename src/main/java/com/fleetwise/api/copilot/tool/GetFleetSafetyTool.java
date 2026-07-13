package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
import com.fleetwise.api.safety.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetFleetSafetyTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final SafetyScoreService safetyScoreService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_fleet_safety";
    }

    @Override
    public String description() {
        return """
                Returns current vehicle safety scores, rankings, hard braking,
                acceleration, harsh turns, speeding, idling, and check-engine data.
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

        List<VehicleSafetyScoreResponse> scores =
                safetyScoreService.getSystemFleetSafetyScores(fleetId);

        try {
            return objectMapper.writeValueAsString(scores);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize fleet safety data",
                    ex
            );
        }
    }
}