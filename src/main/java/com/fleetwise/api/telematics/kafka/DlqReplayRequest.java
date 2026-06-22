package com.fleetwise.api.telematics.kafka;

public record DlqReplayRequest(
        int maxMessages
) {}