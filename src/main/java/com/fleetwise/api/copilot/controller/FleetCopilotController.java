package com.fleetwise.api.copilot.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.copilot.dto.FleetCopilotRequest;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.service.FleetCopilotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fleets/{fleetId}/copilot")
public class FleetCopilotController {

    private final FleetCopilotService fleetCopilotService;

    @PostMapping("/ask")
    public FleetCopilotResponse ask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @Valid @RequestBody FleetCopilotRequest request
    ) {
        return fleetCopilotService.ask(
                principal.getId(),
                fleetId,
                request.question()
        );
    }
}