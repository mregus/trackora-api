package com.fleetwise.api.fuel.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.fuel.dto.*;
import com.fleetwise.api.fuel.service.FuelLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Tag(name = "Fuel", description = "Fuel logging and cost tracking APIs")
@RestController
@RequiredArgsConstructor
public class FuelLogController {
    private final FuelLogService fuelService;

    @Operation(summary = "Create new fuel log for vehicle")
    @PostMapping("/api/vehicles/{vehicleId}/fuel-logs")
    public FuelLogResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CreateFuelLogRequest req) {
        return fuelService.create(vehicleId, principal.getId(), req);
    }

    @Operation(summary = "Inquire vehicle fuel log")
    @GetMapping("/api/vehicles/{vehicleId}/fuel-logs")
    public List<FuelLogResponse> listVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId) {
        return fuelService.getVehicleLogs(vehicleId, principal.getId());
    }

    @Operation(summary = "Inquire fuel log for all vehicles on fleet")
    @GetMapping("/api/fleets/{fleetId}/fuel-logs")
    public List<FuelLogResponse> listFleet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId) {
        return fuelService.getFleetLogs(fleetId, principal.getId());
    }

    @Operation(summary = "Inquire a fuel log")
    @GetMapping("/api/fuel-logs/{id}")
    public FuelLogResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return fuelService.get(id, principal.getId());
    }

    @Operation(summary = "Update a fuel log")
    @PutMapping("/api/fuel-logs/{id}")
    public FuelLogResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFuelLogRequest req) {
        return fuelService.update(id, principal.getId(), req);
    }

    @Operation(summary = "Delete a fuel log")
    @DeleteMapping("/api/fuel-logs/{id}")
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        fuelService.delete(id, principal.getId());
    }
}
