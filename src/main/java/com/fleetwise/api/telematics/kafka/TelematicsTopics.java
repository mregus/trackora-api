package com.fleetwise.api.telematics.kafka;

public final class TelematicsTopics {

    private TelematicsTopics() {}

    public static final String GEOMETRIS_RAW_PACKETS =
            "geometris.raw-packets";

    public static final String GEOMETRIS_RAW_PACKETS_FAILED =
            "geometris.raw-packets.failed";
}