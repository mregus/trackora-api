package com.fleetwise.api.telematics.geometris;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class GeometrisPacketParser {

    public GeometrisPacket parse(String rawPacket) {
        if (rawPacket == null || rawPacket.isBlank()) {
            throw new IllegalArgumentException("Raw Geometris packet is required");
        }

        String[] fields = rawPacket.trim().split(",", -1);

        if (fields.length < 2) {
            throw new IllegalArgumentException("Invalid Geometris packet format");
        }

        if ("F001".equalsIgnoreCase(value(fields, 0))) {
            return parseInventoryPacket(fields);
        }

        return parseGpsTrailPacket(fields);
    }

    private GeometrisPacket parseInventoryPacket(String[] fields) {
        if (fields.length < 24) {
            throw new IllegalArgumentException("Invalid Geometris inventory packet format");
        }

        return new GeometrisPacket(
                value(fields, 0),                           // format crc F001
                value(fields, 1),                           // serial
                value(fields, 2),                           // reason text
                parseEpochSeconds(value(fields, 3)),        // event time
                parseDouble(value(fields, 4)),              // lat
                parseDouble(value(fields, 5)),              // lon
                parseBigDecimal(value(fields, 10)),         // speed
                parseInteger(value(fields, 11)),            // heading
                parseOdometer(value(fields, 12)),           // gps odometer
                parseInteger(value(fields, 15)),            // ignition duration
                parseInteger(value(fields, 16)),            // total idle duration
                parseInteger(value(fields, 17)),            // rpm
                parseInteger(value(fields, 18)),            // coolant temp c
                parseOdometer(value(fields, 20)),           // ecu odometer
                blankToNull(value(fields, 21)),             // vin
                parseBigDecimal(value(fields, 22)),         // fuel %
                blankToNull(value(fields, 23)),             // active dtc
                parseBigDecimal(value(fields, 24)),         // battery/internal/throttle field for now
                null                                              // location trail
        );
    }

    private GeometrisPacket parseGpsTrailPacket(String[] fields) {
        if (fields.length < 12) {
            throw new IllegalArgumentException("Invalid Geometris GPS trail packet format");
        }

        return new GeometrisPacket(
                value(fields, 0),                           // format crc/checksum e.g. 5873
                value(fields, 1),                           // serial
                "GPS_TRAIL",                                      // synthetic reason
                parseEpochSeconds(value(fields, 2)),        // event time
                parseDouble(value(fields, 3)),              // lat
                parseDouble(value(fields, 4)),              // lon
                parseBigDecimal(value(fields, 9)),          // speed
                parseInteger(value(fields, 8)),             // heading
                parseOdometer(value(fields, 10)),           // odometer
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                blankToNull(value(fields, 11))              // location trail
        );
    }

    private String value(String[] fields, int index) {
        if (index >= fields.length) {
            return null;
        }

        return fields[index] == null ? null : fields[index].trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Instant parseEpochSeconds(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Instant.ofEpochSecond(Long.parseLong(value.trim()));
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Integer.parseInt(value.trim());
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value.trim());
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value.trim());
    }

    private BigDecimal parseOdometer(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value.trim().replace("T", ""));
    }
}