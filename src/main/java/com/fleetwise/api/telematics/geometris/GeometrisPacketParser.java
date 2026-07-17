package com.fleetwise.api.telematics.geometris;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeometrisPacketParser {

    private final GeometrisFormatRegistry formatRegistry;
    private final GeometrisCsvFieldParser csvFieldParser;
    private final GeometrisPacketMapper packetMapper;

    public GeometrisPacket parse(String rawPacket) {
        String[] fields = csvFieldParser.split(rawPacket);

        if (fields.length == 0 || fields[0].isBlank()) {
            throw new IllegalArgumentException(
                    "Geometris packet format checksum is missing"
            );
        }

        String checksum = fields[0].toUpperCase();

        GeometrisPacketDefinition definition =
                formatRegistry.getRequired(checksum);

        GeometrisParsedFields parsedFields =
                csvFieldParser.parse(
                        rawPacket,
                        definition
                );

        return packetMapper.map(parsedFields);
    }
}