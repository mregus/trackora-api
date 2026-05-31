package com.fleetwise.api.dashboard.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.dto.FleetRecommendationResponse;
import com.fleetwise.api.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Dashboard", description = "Fleet dashboard and reporting APIs")
@RestController
@RequestMapping("/api/fleets/{fleetId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Inquire fleet summary report")
    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return dashboardService.getSummary(fleetId, principal.getId());
    }

    @Operation(summary = "Get fleet recommendations")
    @GetMapping("/recommendations")
    public List<FleetRecommendationResponse> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return dashboardService.getRecommendations(fleetId, principal.getId());
    }
}