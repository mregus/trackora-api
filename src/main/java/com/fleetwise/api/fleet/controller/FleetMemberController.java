package com.fleetwise.api.fleet.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.fleet.dto.AddFleetMemberRequest;
import com.fleetwise.api.fleet.dto.FleetMemberResponse;
import com.fleetwise.api.fleet.service.FleetMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fleets/{fleetId}/members")
public class FleetMemberController {

    private final FleetMemberService fleetMemberService;

    @GetMapping
    public List<FleetMemberResponse> listMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId
    ) {
        return fleetMemberService.listMembers(fleetId, principal.getId());
    }

    @PostMapping
    public FleetMemberResponse addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @Valid @RequestBody AddFleetMemberRequest request
    ) {
        return fleetMemberService.addMember(
                fleetId,
                principal.getId(),
                request
        );
    }

    @DeleteMapping("/{memberId}")
    public void removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fleetId,
            @PathVariable UUID memberId
    ) {
        fleetMemberService.removeMember(
                fleetId,
                principal.getId(),
                memberId
        );
    }
}