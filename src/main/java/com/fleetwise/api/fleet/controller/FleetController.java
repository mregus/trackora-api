package com.fleetwise.api.fleet.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.fleet.dto.CreateFleetRequest;
import com.fleetwise.api.fleet.dto.FleetResponse;
import com.fleetwise.api.fleet.dto.UpdateFleetRequest;
import com.fleetwise.api.fleet.service.FleetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Fleets", description = "Fleet management APIs")
@RestController
@RequestMapping("/api/fleets")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @Operation(summary = "Create a new fleet")
    @PostMapping
    public FleetResponse createFleet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateFleetRequest request
    ) {
        return fleetService.createFleet(principal.getId(), request);
    }

    @Operation(summary = "Get all fleets on account")
    @GetMapping
    public List<FleetResponse> getMyFleets(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return fleetService.getMyFleets(principal.getId());
    }

    @Operation(summary = "Inquire specific fleet")
    @GetMapping("/{fleetId}")
    public FleetResponse getFleet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return fleetService.getFleet(fleetId, principal.getId());
    }

    @Operation(summary = "Update a fleet")
    @PutMapping("/{fleetId}")
    public FleetResponse updateFleet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @Valid @RequestBody UpdateFleetRequest request
    ) {
        return fleetService.updateFleet(fleetId, principal.getId(), request);
    }

    @Operation(summary = "Delete a fleet")
    @DeleteMapping("/{fleetId}")
    public void deleteFleet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        fleetService.deleteFleet(fleetId, principal.getId());
    }
}