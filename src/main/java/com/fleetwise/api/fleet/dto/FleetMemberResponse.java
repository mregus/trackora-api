package com.fleetwise.api.fleet.dto;

import com.fleetwise.api.fleet.entity.FleetMemberRole;

import java.time.Instant;
import java.util.UUID;

public record FleetMemberResponse(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        FleetMemberRole role,
        Instant createdAt
) {}