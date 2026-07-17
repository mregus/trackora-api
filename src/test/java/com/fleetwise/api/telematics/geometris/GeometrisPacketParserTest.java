package com.fleetwise.api.telematics.geometris;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringJUnitConfig(classes = {
        GeometrisPacketParser.class,
        GeometrisFormatRegistry.class,
        GeometrisCsvFieldParser.class,
        GeometrisPacketMapper.class,
        GeometrisValueParser.class
})
class GeometrisPacketParserTest {

    @Autowired
    private GeometrisPacketParser parser;

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
        String raw =
                "5873,87X061350079,1782164277,29.125814,-82.202898,50,126,N,86,39,128428.9,-3:361:41:2.715:1273:40:8.1282:1517:35:12";

        GeometrisPacket packet = parser.parse(raw);

        assertThat(packet.formatCrc()).isEqualTo("5873");
        assertThat(packet.serialNumber()).isEqualTo("87X061350079");
        assertThat(packet.reasonText()).isNull();
        assertThat(packet.recordedAt())
                .isEqualTo(Instant.ofEpochSecond(1782164277));
        assertThat(packet.latitude()).isEqualTo(29.125814);
        assertThat(packet.longitude()).isEqualTo(-82.202898);
        assertThat(packet.headingDegrees()).isEqualTo(86);
        assertThat(packet.speedMph()).isEqualByComparingTo("39");
        assertThat(packet.gpsOdometerMiles())
                .isEqualByComparingTo("128428.9");
        assertThat(packet.locationTrail()).isNotBlank();
        assertThat(packet.locationTrail()).startsWith("-3:361:41:2");
    }

    @Test
    void shouldParse5873WithLocationTrail() {
        String raw = """
                5873,87X061350079,1784072347,29.126219,-82.201206,50,1751,N,234,31,128562.3,-725:-864:17:10.-1250:-584:30:22
                """.strip();

        GeometrisPacket packet = parser.parse(raw);

        assertThat(packet.formatCrc()).isEqualTo("5873");
        assertThat(packet.serialNumber())
                .isEqualTo("87X061350079");
        assertThat(packet.recordedAt())
                .isEqualTo(
                        Instant.ofEpochSecond(1784072347)
                );
        assertThat(packet.speedMph())
                .isEqualByComparingTo("31");
        assertThat(packet.headingDegrees())
                .isEqualTo(234);
        assertThat(packet.gpsOdometerMiles())
                .isEqualByComparingTo("128562.3");
        assertThat(packet.locationTrail())
                .startsWith("-725:-864");
    }

    @Test
    void shouldParse5873WithEmptyTrail() {
        GeometrisPacket packet = parser.parse(
                "5873,87X061350079,1784072468,29.125616,-82.208694,50,1759,N,7,0,128562.9,"
        );

        assertThat(packet.locationTrail()).isNull();
    }

    @Test
    void shouldRejectUnknownFormatChecksum() {
        assertThatThrownBy(() ->
                parser.parse(
                        "ABCD,87X061350079,1784072468"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Unsupported Geometris format checksum"
                );
    }

    @Test
    void shouldReject5873WhenRequiredFieldsAreMissing() {
        assertThatThrownBy(() ->
                parser.parse(
                        "5873,87X061350079,1784072468,29.125616"
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected at least 12 fields");
    }
}