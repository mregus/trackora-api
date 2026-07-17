package com.fleetwise.api.telematics.geometris;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class GeometrisParsedFields {

    private final Map<GeometrisPacketItem, String> values;

    public GeometrisParsedFields(
            Map<GeometrisPacketItem, String> values
    ) {
        this.values = Collections.unmodifiableMap(
                new EnumMap<>(values)
        );
    }

    public String get(GeometrisPacketItem item) {
        return values.get(item);
    }

    public String getNullable(GeometrisPacketItem item) {
        String value = values.get(item);

        return value == null || value.isBlank()
                ? null
                : value;
    }

    public boolean contains(GeometrisPacketItem item) {
        return values.containsKey(item);
    }

    public Map<GeometrisPacketItem, String> asMap() {
        return values;
    }
}