package com.fleetwise.api.telematics.simulator;

import java.time.Instant;
import java.util.Set;

public record SimulatorStatusResponse(
        boolean running,
        Set<String> serialNumbers,
        int intervalSeconds,
        Instant startedAt,
        long packetsPublished
) {}