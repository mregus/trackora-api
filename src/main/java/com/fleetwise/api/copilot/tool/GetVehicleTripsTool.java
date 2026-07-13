package com.fleetwise.api.copilot.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.copilot.tool.dto.VehicleTripToolResult;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.telematics.dto.TelematicsTripResponse;
import com.fleetwise.api.telematics.service.TripQueryService;
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
public class GetVehicleTripsTool implements FleetCopilotTool {

    private final FleetAccessService fleetAccessService;
    private final VehicleRepository vehicleRepository;
    private final TripQueryService tripQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return "get_vehicle_trips";
    }

    @Override
    public String description() {
        return """
                Returns recent trips for one vehicle, including start and end time,
                duration, distance, average speed, maximum speed, and point count.

                Use this tool for questions about recent travel, longest trips,
                mileage, trip count, driving time, and vehicle activity.
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
                                "Vehicle UUID, license plate, VIN, make, or model"
                        ),
                        "days", Map.of(
                                "type", List.of("integer", "null"),
                                "description",
                                "Recent days to search. Use null to default to 7.",
                                "minimum", 1,
                                "maximum", 90
                        ),
                        "limit", Map.of(
                                "type", List.of("integer", "null"),
                                "description",
                                "Maximum trips to return. Use null to default to 20.",
                                "minimum", 1,
                                "maximum", 100
                        )
                ),
                "required", List.of(
                        "vehicleQuery",
                        "days",
                        "limit"
                ),
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

        String vehicleQuery =
                readRequiredText(arguments, "vehicleQuery");

        int days = readBoundedInt(arguments, "days", 7, 1, 90);
        int limit = readBoundedInt(arguments, "limit", 20, 1, 100);

        Vehicle vehicle = findVehicle(fleetId, vehicleQuery);

        Instant end = Instant.now();
        Instant start = end.minus(days, ChronoUnit.DAYS);

        List<TelematicsTripResponse> trips =
                tripQueryService.getVehicleTripsForSystem(
                        vehicle.getId(),
                        fleetId,
                        start,
                        end,
                        15
                );

        List<VehicleTripToolResult> results = trips.stream()
                .limit(limit)
                .map(trip -> new VehicleTripToolResult(
                        vehicle.getId(),
                        "%s %s".formatted(
                                vehicle.getMake(),
                                vehicle.getModel()
                        ),
                        vehicle.getLicensePlate(),
                        trip.startTime(),
                        trip.endTime(),
                        trip.durationMinutes(),
                        trip.distanceMiles(),
                        trip.avgSpeedMph(),
                        trip.maxSpeedMph(),
                        trip.pointCount()
                ))
                .toList();

        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Unable to serialize vehicle trip data",
                    ex
            );
        }
    }

    private Vehicle findVehicle(
            UUID fleetId,
            String query
    ) {
        UUID vehicleId = tryParseUuid(query);

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
                        query
                );

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No vehicle matched: " + query
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

        return matches.get(0);
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

    private int readBoundedInt(
            JsonNode arguments,
            String field,
            int defaultValue,
            int minimum,
            int maximum
    ) {
        JsonNode value = arguments.get(field);

        if (value == null || value.isNull() || !value.canConvertToInt()) {
            return defaultValue;
        }

        int parsed = value.asInt();

        return Math.max(minimum, Math.min(maximum, parsed));
    }

    private UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}