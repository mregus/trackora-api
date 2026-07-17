package com.fleetwise.api.telematics.geometris;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GeometrisPacketItem {

    FORMAT_CHECKSUM(65),
    SERIAL_NUMBER(28),
    REASON_TEXT(9),
    REASON_CODE(10),
    EVENT_UNIX_TIME(36),
    LATITUDE(3),
    LONGITUDE(4),
    UNIQUE_ID(7),
    LOCATION_AGE(8),
    IGNITION(11),
    DURATION(12),
    SPEED_MPH(14),
    HEADING_DEGREES(17),
    ODOMETER_MILES(24),
    SATELLITES(50),
    IGNITION_DURATION(51),
    WANT_ACK(52),
    TOTAL_IDLE_DURATION(55),
    FENCE_ID(56),
    ENGINE_RPM(70),
    ENGINE_COOLANT_TEMP(71),
    ENGINE_SPEED_MPH(72),
    ENGINE_ODOMETER_MILES(73),
    VIN(74),
    FUEL_LEVEL_PERCENT(75),
    ACTIVE_DTC(76),
    THROTTLE_POSITION_PERCENT(77),
    LOCATION_TRAIL(39),
    CUMULATIVE_FUEL_ECONOMY(80),
    TRIP_FUEL_ECONOMY(81),
    CURRENT_FUEL_ECONOMY(82);

    private final int itemNumber;
}