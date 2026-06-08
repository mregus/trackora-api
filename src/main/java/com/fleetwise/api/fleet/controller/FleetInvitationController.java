package com.fleetwise.api.fleet.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.fleet.dto.AcceptFleetInvitationRequest;
import com.fleetwise.api.fleet.dto.CreateFleetInvitationRequest;
import com.fleetwise.api.fleet.dto.FleetInvitationResponse;
import com.fleetwise.api.fleet.service.FleetInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FleetInvitationController {

    private final FleetInvitationService fleetInvitationService;

    @GetMapping("/api/fleets/{fleetId}/invitations")
    public List<FleetInvitationResponse> listInvitations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return fleetInvitationService.listInvitations(
                fleetId,
                principal.getId()
        );
    }

    @PostMapping("/api/fleets/{fleetId}/invitations")
    public FleetInvitationResponse createInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @Valid @RequestBody CreateFleetInvitationRequest request
    ) {
        return fleetInvitationService.createInvitation(
                fleetId,
                principal.getId(),
                request
        );
    }

    @PostMapping("/api/invitations/accept")
    public Map<String, String> acceptInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AcceptFleetInvitationRequest request
    ) {
        fleetInvitationService.acceptInvitation(
                request.token(),
                principal.getId()
        );

        return Map.of("message", "Invitation accepted successfully.");
    }

    @DeleteMapping("/api/fleets/{fleetId}/invitations/{invitationId}")
    public void cancelInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @PathVariable UUID invitationId
    ) {
        fleetInvitationService.cancelInvitation(
                fleetId,
                principal.getId(),
                invitationId
        );
    }

    @PostMapping("/api/fleets/{fleetId}/invitations/{invitationId}/resend")
    public FleetInvitationResponse resendInvitation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @PathVariable UUID invitationId
    ) {
        return fleetInvitationService.resendInvitation(
                fleetId,
                principal.getId(),
                invitationId
        );
    }
}