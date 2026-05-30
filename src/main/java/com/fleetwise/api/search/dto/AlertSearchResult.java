package com.fleetwise.api.search.dto;

import java.time.Instant;
import java.util.UUID;

public record AlertSearchResult(
        UUID id,
        UUID fleetId,
        UUID vehicleId,
        String label,
        String type,
        String severity,
        boolean resolved,
        Instant createdAt
) {}