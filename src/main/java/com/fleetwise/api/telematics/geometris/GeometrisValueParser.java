package com.fleetwise.api.telematics.geometris;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class GeometrisValueParser {

    public String text(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        return fields.getNullable(item);
    }

    public String requiredText(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        String value = text(fields, item);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Missing required Geometris item: " + item
            );
        }

        return value;
    }

    public Integer integer(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        String value = text(fields, item);

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw invalid(item, value, ex);
        }
    }

    public Long longValue(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        String value = text(fields, item);

        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw invalid(item, value, ex);
        }
    }

    public BigDecimal decimal(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        String value = text(fields, item);

        if (value == null) {
            return null;
        }

        try {
            return new BigDecimal(
                    value.replace("T", "")
            );
        } catch (NumberFormatException ex) {
            throw invalid(item, value, ex);
        }
    }

    public Double doubleValue(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        BigDecimal value = decimal(fields, item);

        return value == null
                ? null
                : value.doubleValue();
    }

    public Instant instant(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        Long epochSeconds = longValue(fields, item);

        return epochSeconds == null
                ? null
                : Instant.ofEpochSecond(epochSeconds);
    }

    public boolean yesNo(
            GeometrisParsedFields fields,
            GeometrisPacketItem item
    ) {
        return "Y".equalsIgnoreCase(text(fields, item));
    }

    private IllegalArgumentException invalid(
            GeometrisPacketItem item,
            String value,
            Exception cause
    ) {
        return new IllegalArgumentException(
                "Invalid value for Geometris item %s: %s"
                        .formatted(item, value),
                cause
        );
    }
}