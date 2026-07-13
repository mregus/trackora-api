package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.copilot.tool.dto.VehicleDetailsToolResult;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.safety.entity.VehicleSafetyScore;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreRepository;
import com.fleetwise.api.telematics.entity.VehicleCurrentState;
import com.fleetwise.api.telematics.repository.VehicleCurrentStateRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetVehicleDetailsTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final VehicleRepository vehicleRepository;
    private final VehicleCurrentStateRepository currentStateRepository;
    private final VehicleSafetyScoreRepository safetyScoreRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_vehicle_details";
    }

    @Override
    public String description() {
        return """
                Returns detailed information for one fleet vehicle, including
                vehicle identity, current telematics state, device status,
                fuel level, speed, check-engine state, and safety score.

                Use this tool when the user asks about a specific vehicle by
                license plate, VIN, vehicle ID, make, or model.
                """;
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "vehicleQuery", Map.of(
                                "type", "string",
                                "description",
                                "Vehicle UUID, license plate, VIN, make/model, or identifying text"
                        )
                ),
                "required", List.of("vehicleQuery"),
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

        String vehicleQuery = readRequiredText(
                arguments,
                "vehicleQuery"
        );

        Vehicle vehicle = findVehicle(fleetId, vehicleQuery);

        VehicleCurrentState currentState =
                currentStateRepository.findById(vehicle.getId())
                        .orElse(null);

        VehicleSafetyScore safetyScore =
                safetyScoreRepository.findByVehicleId(vehicle.getId())
                        .orElse(null);

        VehicleDetailsToolResult result =
                toResult(vehicle, currentState, safetyScore);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize vehicle details",
                    ex
            );
        }
    }

    private Vehicle findVehicle(
            UUID fleetId,
            String vehicleQuery
    ) {
        UUID vehicleId = tryParseUuid(vehicleQuery);

        if (vehicleId != null) {
            return vehicleRepository
                    .findByIdAndFleetId(vehicleId, fleetId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Vehicle not found in this fleet"
                            )
                    );
        }

        List<Vehicle> matches =
                vehicleRepository.findFleetVehiclesForCopilot(
                        fleetId,
                        vehicleQuery
                );

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No vehicle matched: " + vehicleQuery
            );
        }

        if (matches.size() > 1) {
            String options = matches.stream()
                    .limit(5)
                    .map(vehicle -> "%s %s (%s)".formatted(
                            vehicle.getMake(),
                            vehicle.getModel(),
                            vehicle.getLicensePlate()
                    ))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");

            throw new IllegalArgumentException(
                    "Multiple vehicles matched. Be more specific. Matches: "
                            + options
            );
        }

        return matches.getFirst();
    }

    private VehicleDetailsToolResult toResult(
            Vehicle vehicle,
            VehicleCurrentState state,
            VehicleSafetyScore safety
    ) {
        return new VehicleDetailsToolResult(
                vehicle.getId(),
                "%s %s".formatted(
                        vehicle.getMake(),
                        vehicle.getModel()
                ),
                vehicle.getLicensePlate(),
                vehicle.getVin(),
                vehicle.getStatus() == null
                        ? null
                        : vehicle.getStatus().name(),

                state == null ? null : state.getLatitude(),
                state == null ? null : state.getLongitude(),
                state == null ? null : state.getSpeedMph(),
                state == null ? null : state.getFuelLevelPercent(),
                state == null ? null : state.getHeadingDegrees(),
                state != null && state.isCheckEngine(),
                state == null ? null : state.getLastSeenAt(),
                determineDeviceStatus(
                        state == null ? null : state.getLastSeenAt()
                ),

                safety == null ? null : safety.getScore(),
                safety == null ? null : safety.getHardBrakes(),
                safety == null ? null : safety.getHardAccelerations(),
                safety == null ? null : safety.getHarshTurns(),
                safety == null ? null : safety.getSpeedingEvents(),
                safety == null ? null : safety.getIdleMinutes()
        );
    }

    private String readRequiredText(
            JsonNode arguments,
            String field
    ) {
        JsonNode value = arguments.get(field);

        if (value == null || value.asText().isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        return value.asText().trim();
    }

    private UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String determineDeviceStatus(Instant lastSeenAt) {
        if (lastSeenAt == null) {
            return "OFFLINE";
        }

        long minutes =
                ChronoUnit.MINUTES.between(
                        lastSeenAt,
                        Instant.now()
                );

        if (minutes <= 5) {
            return "ONLINE";
        }

        if (minutes <= 15) {
            return "STALE";
        }

        return "OFFLINE";
    }
}