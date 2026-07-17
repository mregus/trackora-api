package com.fleetwise.api.telematics.geometris;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fleetwise.api.telematics.geometris.GeometrisPacketItem.*;

@Component
public class GeometrisFormatRegistry {

    private final Map<String, GeometrisPacketDefinition> definitions;

    public GeometrisFormatRegistry() {
        Map<String, GeometrisPacketDefinition> formats =
                new LinkedHashMap<>();

        formats.put(
                "F001",
                new GeometrisPacketDefinition(
                        "F001",
                        List.of(
                                FORMAT_CHECKSUM,                // 65
                                SERIAL_NUMBER,                  // 28
                                REASON_TEXT,                    // 9
                                EVENT_UNIX_TIME,                // 36
                                LATITUDE,                       // 3
                                LONGITUDE,                      // 4
                                UNIQUE_ID,                      // 7
                                LOCATION_AGE,                   // 8
                                IGNITION,                       // 11
                                DURATION,                       // 12
                                SPEED_MPH,                      // 14
                                HEADING_DEGREES,                // 17
                                ODOMETER_MILES,                 // 24
                                SATELLITES,                     // 50
                                FENCE_ID,                       // 56
                                IGNITION_DURATION,              // 51
                                TOTAL_IDLE_DURATION,            // 55
                                ENGINE_RPM,                     // 70
                                ENGINE_COOLANT_TEMP,            // 71
                                ENGINE_SPEED_MPH,               // 72
                                ENGINE_ODOMETER_MILES,          // 73
                                VIN,                            // 74
                                FUEL_LEVEL_PERCENT,             // 75
                                ACTIVE_DTC,                     // 76
                                THROTTLE_POSITION_PERCENT,      // 77
                                CUMULATIVE_FUEL_ECONOMY,        // 80
                                TRIP_FUEL_ECONOMY,              // 81
                                CURRENT_FUEL_ECONOMY            // 82
                        )
                )
        );

        formats.put(
                "5873",
                new GeometrisPacketDefinition(
                        "5873",
                        List.of(
                                FORMAT_CHECKSUM,
                                SERIAL_NUMBER,
                                EVENT_UNIX_TIME,
                                LATITUDE,
                                LONGITUDE,
                                SATELLITES,
                                IGNITION_DURATION,
                                WANT_ACK,
                                HEADING_DEGREES,
                                SPEED_MPH,
                                ODOMETER_MILES,
                                LOCATION_TRAIL
                        )
                )
        );

        this.definitions = Map.copyOf(formats);
    }

    public Optional<GeometrisPacketDefinition> find(
            String formatChecksum
    ) {
        if (formatChecksum == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                definitions.get(formatChecksum.toUpperCase())
        );
    }

    public GeometrisPacketDefinition getRequired(
            String formatChecksum
    ) {
        return find(formatChecksum)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Unsupported Geometris format checksum: "
                                        + formatChecksum
                        )
                );
    }

    public Map<String, GeometrisPacketDefinition> all() {
        return definitions;
    }
}