package com.fleetwise.api.telematics.geometris;

import java.time.Instant;
import java.util.UUID;

public record GeometrisRawPacketResponse(
        UUID id,
        String serialNumber,
        String reasonText,
        boolean parsedSuccessfully,
        String errorMessage,
        Instant receivedAt
) {}
