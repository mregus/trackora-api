package com.fleetwise.api.fleet.dto;

import com.fleetwise.api.fleet.entity.FleetMemberRole;

import java.time.Instant;
import java.util.UUID;

public record FleetInvitationResponse(

        UUID id,

        String email,

        FleetMemberRole role,

        boolean accepted,

        Instant expiresAt,

        Instant createdAt

) {}