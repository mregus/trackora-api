package com.fleetwise.api.telematics.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.common.exception.ForbiddenException;
import com.fleetwise.api.telematics.dto.*;
import com.fleetwise.api.telematics.geometris.GeometrisRawPacketResponse;
import com.fleetwise.api.telematics.kafka.GeometrisPacketProducer;
import com.fleetwise.api.telematics.service.TelematicsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TelematicsController {

    @Value("${telematics.geometris.api-key}")
    private String geometrisApiKey;

    private final TelematicsService telematicsService;
    private final Optional<GeometrisPacketProducer> geometrisPacketProducer;

    @PostMapping("/api/telematics/events")
    public TelematicsEventResponse createEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTelematicsEventRequest request
    ) {
        return telematicsService.createEvent(principal.getId(), request);
    }

    @GetMapping("/api/vehicles/{vehicleId}/telematics/latest")
    public TelematicsEventResponse getLatestForVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return telematicsService.getLatestForVehicle(principal.getId(), vehicleId);
    }

    @PostMapping("/api/telematics/devices")
    public TelematicsDeviceResponse registerDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterTelematicsDeviceRequest request
    ) {
        return telematicsService.registerDevice(principal.getId(), request);
    }

    @GetMapping("/api/vehicles/{vehicleId}/telematics/devices")
    public List<TelematicsDeviceResponse> getDevicesForVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return telematicsService.getDevicesForVehicle(principal.getId(), vehicleId);
    }

    @PostMapping(
            value = "/api/telematics/providers/geometris/packets",
            consumes = "text/plain"
    )
    public Map<String, String> ingestGeometrisPacket(
            @RequestHeader("X-GEOMETRIS-API-KEY") String apiKey,
            @RequestBody String rawPacket
    ) {
        if (!geometrisApiKey.equals(apiKey)) {
            throw new ForbiddenException("Invalid Geometris API key");
        }

        if (geometrisPacketProducer.isPresent()) {
            geometrisPacketProducer.get().publish(rawPacket);
            return Map.of("message", "Packet accepted for processing");
        }

        telematicsService.ingestGeometrisPacket(rawPacket);
        return Map.of("message", "Packet processed");
    }

    @GetMapping("/api/fleets/{fleetId}/telematics/latest")
    public List<FleetTelematicsLocationResponse> getLatestFleetLocations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return telematicsService.getLatestFleetLocations(
                principal.getId(),
                fleetId
        );
    }

    @GetMapping("/api/vehicles/{vehicleId}/telematics/history")
    public List<TelematicsHistoryPointResponse> getVehicleHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return telematicsService.getVehicleHistory(principal.getId(), vehicleId);
    }

    @GetMapping("/api/telematics/providers/geometris/raw-packets")
    public List<GeometrisRawPacketResponse> getLatestGeometrisRawPackets() {
        return telematicsService.getLatestGeometrisRawPackets();
    }

    @GetMapping("/api/telematics/providers/geometris/raw-packets/failed")
    public List<GeometrisRawPacketResponse> getFailedGeometrisRawPackets() {
        return telematicsService.getFailedGeometrisRawPackets();
    }
}