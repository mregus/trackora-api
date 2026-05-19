package com.fleetwise.api.fleet.dto;

import java.time.Instant;
import java.util.UUID;

public record FleetResponse(
        UUID id,
        String name,
        UUID ownerUserId,
        Instant createdAt,
        Instant updatedAt
) {}