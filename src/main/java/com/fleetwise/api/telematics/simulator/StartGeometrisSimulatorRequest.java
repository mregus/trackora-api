package com.fleetwise.api.telematics.simulator;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record StartGeometrisSimulatorRequest(
        @NotEmpty
        Set<String> serialNumbers,

        @Min(1)
        Integer intervalSeconds
) {}