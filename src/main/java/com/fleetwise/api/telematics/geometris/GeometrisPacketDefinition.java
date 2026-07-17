package com.fleetwise.api.telematics.geometris;

import java.util.List;

public record GeometrisPacketDefinition(
        String formatChecksum,
        List<GeometrisPacketItem> items
) {

    public GeometrisPacketDefinition {
        items = List.copyOf(items);

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Packet definition must contain at least one item"
            );
        }

        if (items.get(0) != GeometrisPacketItem.FORMAT_CHECKSUM) {
            throw new IllegalArgumentException(
                    "Packet definition must begin with FORMAT_CHECKSUM"
            );
        }
    }
}