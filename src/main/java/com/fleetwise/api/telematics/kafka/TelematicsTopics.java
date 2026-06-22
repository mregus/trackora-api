package com.fleetwise.api.telematics.kafka;

public final class TelematicsTopics {

    private TelematicsTopics() {}

    public static final String DEVICE_TELEMETRY = "device-telemetry";
    public static final String DEVICE_TELEMETRY_DLQ = "device-telemetry.dlq";
}