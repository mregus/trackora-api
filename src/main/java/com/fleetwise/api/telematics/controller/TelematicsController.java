package com.fleetwise.api.telematics.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.telematics.dto.CreateTelematicsEventRequest;
import com.fleetwise.api.telematics.dto.TelematicsEventResponse;
import com.fleetwise.api.telematics.service.TelematicsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TelematicsController {

    private final TelematicsService telematicsService;

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
}