package com.fleetwise.api.telematics.geometris;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@Component
public class GeometrisCsvFieldParser {

    public GeometrisParsedFields parse(
            String rawPacket,
            GeometrisPacketDefinition definition
    ) {
        String[] fields = split(rawPacket);

        int expected = definition.items().size();

        if (fields.length < expected) {
            throw new IllegalArgumentException(
                    "Invalid Geometris packet for format %s. Expected at least %d fields, got %d"
                            .formatted(
                                    definition.formatChecksum(),
                                    expected,
                                    fields.length
                            )
            );
        }

        Map<GeometrisPacketItem, String> values =
                new EnumMap<>(GeometrisPacketItem.class);

        for (int index = 0; index < expected; index++) {
            GeometrisPacketItem item =
                    definition.items().get(index);

            values.put(item, normalize(fields[index]));
        }

        return new GeometrisParsedFields(values);
    }

    public String[] split(String rawPacket) {
        if (rawPacket == null || rawPacket.isBlank()) {
            throw new IllegalArgumentException(
                    "Raw Geometris packet is required"
            );
        }

        return Arrays.stream(
                        rawPacket.strip().split(",", -1)
                )
                .map(String::trim)
                .toArray(String[]::new);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}