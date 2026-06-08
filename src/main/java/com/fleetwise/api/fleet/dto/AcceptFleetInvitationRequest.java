package com.fleetwise.api.fleet.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptFleetInvitationRequest(
        @NotBlank
        String token
) {}