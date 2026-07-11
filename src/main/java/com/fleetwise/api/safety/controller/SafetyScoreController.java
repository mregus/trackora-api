package com.fleetwise.api.safety.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.safety.dto.FleetSafetyTrendPoint;
import com.fleetwise.api.safety.dto.SafetyInsightResponse;
import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
import com.fleetwise.api.safety.service.SafetyInsightService;
import com.fleetwise.api.safety.service.SafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fleets/{fleetId}/safety-scores")
public class SafetyScoreController {

    private final SafetyScoreService safetyScoreService;
    private final SafetyInsightService safetyInsightService;

    @GetMapping
    public List<VehicleSafetyScoreResponse> getScores(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return safetyScoreService.getFleetSafetyScores(
                principal.getId(),
                fleetId
        );
    }

    @GetMapping("/trends")
    public List<FleetSafetyTrendPoint> getTrend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return safetyScoreService.getFleetSafetyTrend(
                principal.getId(),
                fleetId,
                days
        );
    }

    @GetMapping("/insights")
    public SafetyInsightResponse getSafetyInsight(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return safetyInsightService.getInsight(
                principal.getId(),
                fleetId
        );
    }
}