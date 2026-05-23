package com.fleetwise.api.vehicle.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.vehicle.dto.*;
import com.fleetwise.api.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Vehicles", description = "Vehicle management APIs")
@RestController
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Create vehicle on a fleet")
    @PostMapping("/api/fleets/{fleetId}/vehicles")
    public VehicleResponse createVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        return vehicleService.createVehicle(
                fleetId,
                principal.getId(),
                request
        );
    }

    @Operation(summary = "Inquire vehicles on a fleet")
    @GetMapping("/api/fleets/{fleetId}/vehicles")
    public List<VehicleResponse> getFleetVehicles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return vehicleService.getFleetVehicles(
                fleetId,
                principal.getId()
        );
    }

    @Operation(summary = "Inquire vehicle by id")
    @GetMapping("/api/vehicles/{vehicleId}")
    public VehicleResponse getVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return vehicleService.getVehicle(
                vehicleId,
                principal.getId()
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Update a vehicle")
    @PutMapping("/api/vehicles/{vehicleId}")
    public VehicleResponse updateVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        return vehicleService.updateVehicle(
                vehicleId,
                principal.getId(),
                request
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Delete a vehicle")
    @DeleteMapping("/api/vehicles/{vehicleId}")
    public void deleteVehicle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        vehicleService.deleteVehicle(
                vehicleId,
                principal.getId()
        );
    }
}