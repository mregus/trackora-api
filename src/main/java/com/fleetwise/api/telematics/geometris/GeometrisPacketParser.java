package com.fleetwise.api.telematics.geometris;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class GeometrisPacketParser {

    public GeometrisPacket parse(String rawPacket) {
        if (rawPacket == null || rawPacket.isBlank()) {
            throw new IllegalArgumentException("Geometris packet is required");
        }

        String[] fields = rawPacket.trim().split(",", -1);

        if (fields.length < 18) {
            throw new IllegalArgumentException("Invalid Geometris packet format");
        }

        return new GeometrisPacket(
                value(fields, 0),
                value(fields, 1),
                value(fields, 2),
                toInstant(value(fields, 3)),
                toDouble(value(fields, 4)),
                toDouble(value(fields, 5)),
                toBigDecimal(value(fields, 6)),
                toInteger(value(fields, 7)),
                toBigDecimal(value(fields, 8)),
                toInteger(value(fields, 9)),
                toInteger(value(fields, 10)),
                toInteger(value(fields, 11)),
                toInteger(value(fields, 12)),
                toBigDecimal(cleanTrailingText(value(fields, 13))),
                value(fields, 14),
                toBigDecimal(value(fields, 15)),
                value(fields, 16),
                millivoltsToVolts(value(fields, 17)),
                fields.length > 18 ? value(fields, 18) : null
        );
    }

    private String value(String[] fields, int index) {
        String value = fields[index];

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Instant toInstant(String value) {
        if (value == null) {
            return Instant.now();
        }

        return Instant.ofEpochSecond(Long.parseLong(value));
    }

    private Double toDouble(String value) {
        return value == null ? null : Double.valueOf(value);
    }

    private Integer toInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }

    private BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private String cleanTrailingText(String value) {
        if (value == null) {
            return null;
        }

        return value.replaceAll("[^0-9.\\-]", "");
    }

    private BigDecimal millivoltsToVolts(String value) {
        if (value == null) {
            return null;
        }

        return new BigDecimal(value)
                .divide(BigDecimal.valueOf(1000));
    }
}