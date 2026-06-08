package com.fleetwise.api.fleet.dto;

import com.fleetwise.api.fleet.entity.FleetMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record AddFleetMemberRequest(
        @Email
        @NotNull
        String email,

        @NotNull
        FleetMemberRole role
) {}