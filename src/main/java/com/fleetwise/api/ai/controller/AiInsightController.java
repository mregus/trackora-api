package com.fleetwise.api.ai.controller;

import com.fleetwise.api.ai.dto.*;
import com.fleetwise.api.ai.service.AiInsightService;
import com.fleetwise.api.auth.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "AI Insights", description = "AI-generated fleet insights APIs")
@RestController
@RequiredArgsConstructor
public class AiInsightController {

    private final AiInsightService aiInsightService;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Operation(summary = "Generate fleet summary")
    @PostMapping("/api/fleets/{fleetId}/ai/summary")
    public AiInsightResponse generateFleetSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @RequestBody GenerateAiSummaryRequest request
    ) {
        return aiInsightService.generateFleetSummary(fleetId, principal.getId(), request);
    }

    @Operation(summary = "Get fleet AI insights")
    @GetMapping("/api/fleets/{fleetId}/ai/insights")
    public List<AiInsightResponse> getFleetInsights(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return aiInsightService.listInsights(fleetId, principal.getId());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @PostMapping("/api/vehicles/{vehicleId}/ai/summary")
    public AiInsightResponse generateVehicleSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId,
            @RequestBody GenerateAiSummaryRequest request
    ) {
        return aiInsightService.generateVehicleSummary(
                vehicleId,
                principal.getId(),
                request
        );
    }

    @GetMapping("/api/vehicles/{vehicleId}/ai/insights/latest")
    public AiInsightResponse getLatestVehicleInsight(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return aiInsightService.getLatestVehicleInsight(
                vehicleId,
                principal.getId()
        );
    }

    @GetMapping("/api/vehicles/{vehicleId}/ai/insights")
    public List<AiInsightResponse> getVehicleInsights(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID vehicleId
    ) {
        return aiInsightService.listVehicleInsights(
                vehicleId,
                principal.getId()
        );
    }
}
