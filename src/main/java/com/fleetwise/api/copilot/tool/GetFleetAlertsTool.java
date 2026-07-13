package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.copilot.tool.dto.FleetAlertToolResult;
import com.fleetwise.api.fleet.service.FleetAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetFleetAlertsTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_fleet_alerts";
    }

    @Override
    public String description() {
        return """
                Returns unresolved fleet alerts including severity, type,
                vehicle, message, and creation time. Use this tool for
                questions about critical alerts, warning alerts, device health,
                check-engine events, safety events, and alert priorities.
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
    @Transactional(readOnly = true)
    public String execute(
            UUID fleetId,
            UUID userId,
            JsonNode arguments
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        List<FleetAlertToolResult> results =
                alertRepository.findOpenFleetAlertsForCopilot(fleetId)
                        .stream()
                        .limit(100)
                        .map(this::toResult)
                        .toList();

        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize fleet alert data",
                    ex
            );
        }
    }

    private FleetAlertToolResult toResult(Alert alert) {
        var vehicle = alert.getVehicle();

        return new FleetAlertToolResult(
                alert.getId(),
                vehicle.getId(),
                "%s %s".formatted(
                        vehicle.getMake(),
                        vehicle.getModel()
                ),
                vehicle.getLicensePlate(),
                alert.getType() == null
                        ? null
                        : alert.getType().name(),
                alert.getSeverity() == null
                        ? null
                        : alert.getSeverity().name(),
                alert.getMessage(),
                alert.isResolved(),
                alert.getCreatedAt()
        );
    }
}