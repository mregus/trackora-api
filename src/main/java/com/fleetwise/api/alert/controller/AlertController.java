package com.fleetwise.api.alert.controller;

import com.fleetwise.api.alert.dto.AlertResponse;
import com.fleetwise.api.alert.service.AlertService;
import com.fleetwise.api.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "Alerts", description = "Fleet operational alerts APIs")
@RestController
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "Inquire fleet alerts")
    @GetMapping("/api/fleets/{fleetId}/alerts")
    public List<AlertResponse> getFleetAlerts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return alertService.getFleetAlerts(fleetId, principal.getId());
    }

    @Operation(summary = "Resolve an alert")
    @PutMapping("/api/alerts/{alertId}/resolve")
    public AlertResponse resolveAlert(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID alertId
    ) {
        return alertService.resolve(alertId, principal.getId());
    }
}
