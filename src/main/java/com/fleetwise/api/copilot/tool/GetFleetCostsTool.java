package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.copilot.tool.dto.FleetCostToolResult;
import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.service.DashboardService;
import com.fleetwise.api.fleet.service.FleetAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetFleetCostsTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_fleet_costs";
    }

    @Override
    public String description() {
        return """
                Returns recorded fleet fuel and maintenance costs and their combined
                total. Use this tool for questions about fleet spending, operating
                expenses, fuel costs, maintenance costs, and cost comparisons.
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

        DashboardSummaryResponse summary =
                dashboardService.getSystemSummary(fleetId);

        BigDecimal fuelCost = defaultZero(
                summary.monthlyFuelCost()
        );

        BigDecimal maintenanceCost = defaultZero(
                summary.monthlyMaintenanceCost()
        );

        FleetCostToolResult result = new FleetCostToolResult(
                summary.fleetId(),
                summary.fleetName(),
                fuelCost,
                maintenanceCost,
                fuelCost.add(maintenanceCost),
                summary.totalVehicles()
        );

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize fleet cost data",
                    ex
            );
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}