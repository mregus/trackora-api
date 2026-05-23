package com.fleetwise.api.maintenance.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.maintenance.dto.*;
import com.fleetwise.api.maintenance.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Tag(name = "Maintenance", description = "Vehicle maintenance tracking APIs")
@RestController
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Create a new vehicle maintenance")
    @PostMapping("/api/vehicles/{vehicleId}/maintenance")
    public MaintenanceResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CreateMaintenanceRequest request
    ) {
        return maintenanceService.create(vehicleId, principal.getId(), request);
    }

    @Operation(summary = "Inquire a vehicle maintenance")
    @GetMapping("/api/vehicles/{vehicleId}/maintenance")
    public List<MaintenanceResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return maintenanceService.getVehicleRecords(vehicleId, principal.getId());
    }

    @Operation(summary = "Inquire a vehicle maintenance by id")
    @GetMapping("/api/maintenance/{id}")
    public MaintenanceResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        return maintenanceService.getRecord(id, principal.getId());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Update a vehicle maintenance")
    @PutMapping("/api/maintenance/{id}")
    public MaintenanceResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceRequest request
    ) {
        return maintenanceService.update(id, principal.getId(), request);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Delete a vehicle maintenance")
    @DeleteMapping("/api/maintenance/{id}")
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        maintenanceService.delete(id, principal.getId());
    }
}
