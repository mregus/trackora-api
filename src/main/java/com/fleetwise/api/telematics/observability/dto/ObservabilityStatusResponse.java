package com.fleetwise.api.telematics.observability.dto;

public record ObservabilityStatusResponse(
        String healthStatus,
        double packetsReceived,
        double packetsProcessed,
        double packetsFailed,
        double serviceBusReceived,
        double serviceBusCompleted,
        double serviceBusFailed,
        double dlqDepth,
        double vehiclesOnline,
        double vehiclesStale,
        double vehiclesOffline,
        double activeAlerts
) {}