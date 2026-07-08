package com.fleetwise.api.safety.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
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
}