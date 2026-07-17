package com.fleetwise.api.telematics.geometris;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeometrisPacketMapper {

    private final GeometrisValueParser valueParser;

    public GeometrisPacket map(
            GeometrisParsedFields fields
    ) {
        String reasonText = valueParser.text(
                fields,
                GeometrisPacketItem.REASON_TEXT
        );

        Integer reasonCode = valueParser.integer(
                fields,
                GeometrisPacketItem.REASON_CODE
        );

        if (reasonText == null && reasonCode != null) {
            reasonText = "REASON_CODE_" + reasonCode;
        }

        return new GeometrisPacket(
                mapIgnition(fields),
                valueParser.requiredText(
                        fields,
                        GeometrisPacketItem.FORMAT_CHECKSUM
                ),
                valueParser.requiredText(
                        fields,
                        GeometrisPacketItem.SERIAL_NUMBER
                ),
                reasonText,
                valueParser.instant(
                        fields,
                        GeometrisPacketItem.EVENT_UNIX_TIME
                ),
                valueParser.doubleValue(
                        fields,
                        GeometrisPacketItem.LATITUDE
                ),
                valueParser.doubleValue(
                        fields,
                        GeometrisPacketItem.LONGITUDE
                ),
                valueParser.decimal(
                        fields,
                        GeometrisPacketItem.SPEED_MPH
                ),
                valueParser.integer(
                        fields,
                        GeometrisPacketItem.HEADING_DEGREES
                ),
                valueParser.decimal(
                        fields,
                        GeometrisPacketItem.ODOMETER_MILES
                ),
                valueParser.integer(
                        fields,
                        GeometrisPacketItem.IGNITION_DURATION
                ),
                valueParser.integer(
                        fields,
                        GeometrisPacketItem.TOTAL_IDLE_DURATION
                ),
                valueParser.integer(
                        fields,
                        GeometrisPacketItem.ENGINE_RPM
                ),
                valueParser.integer(
                        fields,
                        GeometrisPacketItem.ENGINE_COOLANT_TEMP
                ),
                valueParser.decimal(
                        fields,
                        GeometrisPacketItem.ENGINE_ODOMETER_MILES
                ),
                valueParser.text(
                        fields,
                        GeometrisPacketItem.VIN
                ),
                valueParser.decimal(
                        fields,
                        GeometrisPacketItem.FUEL_LEVEL_PERCENT
                ),
                valueParser.text(
                        fields,
                        GeometrisPacketItem.ACTIVE_DTC
                ),
                valueParser.decimal(
                        fields,
                        GeometrisPacketItem.THROTTLE_POSITION_PERCENT
                ),
                valueParser.text(
                        fields,
                        GeometrisPacketItem.LOCATION_TRAIL
                )
        );
    }

    private Boolean mapIgnition(
            GeometrisParsedFields fields
    ) {
        Integer ignition = valueParser.integer(
                fields,
                GeometrisPacketItem.IGNITION
        );

        return ignition == null
                ? null
                : ignition == 1;
    }
}