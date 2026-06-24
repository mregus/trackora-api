package com.fleetwise.api.telematics.geometris;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GeometrisPacketParserTest {

    private final GeometrisPacketParser parser = new GeometrisPacketParser();

    @Test
    void parse_ShouldParseReal87InventoryPacket() {
        String raw = "F001,87X061350079,HEADING,1782161645,29.131252,-82.194018,96,0,1,0,6,227,128426.5,6,,3314,941,953,81,6,128426.4T,KNDJN2A21E7088217,55,1:1:P0130,16,0,0,0";

        GeometrisPacket packet = parser.parse(raw);

        assertThat(packet.formatCrc()).isEqualTo("F001");
        assertThat(packet.serialNumber()).isEqualTo("87X061350079");
        assertThat(packet.reasonText()).isEqualTo("HEADING");
        assertThat(packet.recordedAt()).isEqualTo(Instant.ofEpochSecond(1782161645));
        assertThat(packet.latitude()).isEqualTo(29.131252);
        assertThat(packet.longitude()).isEqualTo(-82.194018);
        assertThat(packet.speedMph()).isEqualByComparingTo("6");
        assertThat(packet.headingDegrees()).isEqualTo(227);
        assertThat(packet.gpsOdometerMiles()).isEqualByComparingTo("128426.5");
        assertThat(packet.ignitionDurationSeconds()).isEqualTo(3314);
        assertThat(packet.totalIdleDurationSeconds()).isEqualTo(941);
        assertThat(packet.engineRpm()).isEqualTo(953);
        assertThat(packet.coolantTempC()).isEqualTo(81);
        assertThat(packet.ecuOdometerMiles()).isEqualByComparingTo("128426.4");
        assertThat(packet.vin()).isEqualTo("KNDJN2A21E7088217");
        assertThat(packet.fuelLevelPercent()).isEqualByComparingTo("55");
        assertThat(packet.activeDtc()).isEqualTo("1:1:P0130");
        assertThat(packet.locationTrail()).isNull();
    }

    @Test
    void parse_ShouldParseReal87GpsTrailPacket() {
        String raw = "5873,87X061350079,1782164277,29.125814,-82.202898,50,126,N,86,39,128428.9,-3:361:41:2.715:1273:40:8.1282:1517:35:12.236:984:35:6.-99:804:35:5.-378:771:29:6.-452:592:9:7.-33:19:8.-59:3:11:2.-24:-38:7.2:-50:10.972:-1827:35:15.-77:-851:35:5.-353:-767:37:5.-1503:-1730:35:14.-264:-1079:33:7.-174:-1118:36:7.-482:-905:29:7";

        GeometrisPacket packet = parser.parse(raw);

        assertThat(packet.formatCrc()).isEqualTo("5873");
        assertThat(packet.serialNumber()).isEqualTo("87X061350079");
        assertThat(packet.reasonText()).isEqualTo("GPS_TRAIL");
        assertThat(packet.recordedAt()).isEqualTo(Instant.ofEpochSecond(1782164277));
        assertThat(packet.latitude()).isEqualTo(29.125814);
        assertThat(packet.longitude()).isEqualTo(-82.202898);
        assertThat(packet.headingDegrees()).isEqualTo(86);
        assertThat(packet.speedMph()).isEqualByComparingTo("39");
        assertThat(packet.gpsOdometerMiles()).isEqualByComparingTo("128428.9");
        assertThat(packet.locationTrail()).isNotBlank();
        assertThat(packet.locationTrail()).startsWith("-3:361:41:2");
    }
}