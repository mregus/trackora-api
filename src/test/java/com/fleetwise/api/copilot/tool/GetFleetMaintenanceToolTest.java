package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.copilot.ai.FleetCopilotOpenAiClient;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.entity.MaintenanceStatus;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GetFleetMaintenanceToolTest {

    private final FleetAccessService fleetAccessService =
            mock(FleetAccessService.class);

    private final MaintenanceRepository maintenanceRepository =
            mock(MaintenanceRepository.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    private final GetFleetMaintenanceTool tool =
            new GetFleetMaintenanceTool(
                    fleetAccessService,
                    maintenanceRepository,
                    objectMapper
            );

    @Test
    void shouldReturnOverdueMaintenanceRecords() throws Exception {
        UUID fleetId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        vehicle.setMake("Ford");
        vehicle.setModel("Focus ST");
        vehicle.setLicensePlate("DEMO-003");

        Maintenance maintenance = new Maintenance();
        maintenance.setId(UUID.randomUUID());
        maintenance.setVehicle(vehicle);
        maintenance.setDescription("Oil change");
        maintenance.setServiceDate(LocalDate.now().minusDays(5));
        maintenance.setStatus(MaintenanceStatus.SCHEDULED);
        maintenance.setCost(BigDecimal.valueOf(89.99));

        when(maintenanceRepository.findFleetMaintenanceForCopilot(fleetId))
                .thenReturn(List.of(maintenance));

        String output = tool.execute(
                fleetId,
                userId,
                objectMapper.createObjectNode()
        );

        JsonNode json = objectMapper.readTree(output);

        verify(fleetAccessService).validateAccess(fleetId, userId);
        verify(maintenanceRepository)
                .findFleetMaintenanceForCopilot(fleetId);

        assertThat(json.isArray()).isTrue();
        assertThat(json).hasSize(1);
        assertThat(json.get(0).get("vehicleName").asText())
                .isEqualTo("Ford Focus ST");
        assertThat(json.get(0).get("overdue").asBoolean())
                .isTrue();
        assertThat(json.get(0).get("dueSoon").asBoolean())
                .isFalse();
    }
}