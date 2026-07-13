package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.copilot.tool.dto.FleetMaintenanceToolResult;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.entity.MaintenanceStatus;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetFleetMaintenanceTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final MaintenanceRepository maintenanceRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_fleet_maintenance";
    }

    @Override
    public String description() {
        return """
                Returns fleet maintenance records with vehicle, service date,
                status, cost, overdue status, and whether service is due soon.
                Use this tool for questions about maintenance priorities,
                upcoming service, overdue work, repair history, and service cost.
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

        LocalDate today = LocalDate.now();

        List<FleetMaintenanceToolResult> results =
                maintenanceRepository.findFleetMaintenanceForCopilot(fleetId)
                        .stream()
                        .map(record -> toResult(record, today))
                        .toList();

        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize fleet maintenance data",
                    ex
            );
        }
    }

    private FleetMaintenanceToolResult toResult(
            Maintenance record,
            LocalDate today
    ) {
        LocalDate serviceDate = record.getServiceDate();

        boolean completed =
                record.getStatus() == MaintenanceStatus.COMPLETED;

        boolean overdue =
                !completed
                        && serviceDate != null
                        && serviceDate.isBefore(today);

        boolean dueSoon =
                !completed
                        && serviceDate != null
                        && !serviceDate.isBefore(today)
                        && !serviceDate.isAfter(today.plusDays(7));

        return new FleetMaintenanceToolResult(
                record.getId(),
                record.getVehicle().getId(),
                "%s %s".formatted(
                        record.getVehicle().getMake(),
                        record.getVehicle().getModel()
                ),
                record.getVehicle().getLicensePlate(),
                getServiceType(record),
                serviceDate,
                record.getStatus() == null
                        ? null
                        : record.getStatus().name(),
                record.getCost(),
                overdue,
                dueSoon
        );
    }

    private String getServiceType(Maintenance record) {
        /*
         * Replace this with the actual field from your entity.
         *
         * Examples:
         * return record.getServiceType();
         * return record.getDescription();
         * return record.getServicePerformed();
         */
        return record.getDescription();
    }
}