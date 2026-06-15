package com.fleetwise.api.telematics.geometris;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GeometrisPacketParserTest {

    private final GeometrisPacketParser parser = new GeometrisPacketParser();

    @Test
    void parse_ShouldParseTrackoraGeometrisPacket() {
        String packet =
                "F001,81A161260005,LIVE,1781120000,28.5383,-81.3792,65,180,112450.2,3500,240,2200,89,112449.8,1FADP3L9XGL274054,55,0:0,12450";

        GeometrisPacket result = parser.parse(packet);

        assertThat(result.formatCrc()).isEqualTo("F001");
        assertThat(result.serialNumber()).isEqualTo("81A161260005");
        assertThat(result.reasonText()).isEqualTo("LIVE");
        assertThat(result.latitude()).isEqualTo(28.5383);
        assertThat(result.longitude()).isEqualTo(-81.3792);
        assertThat(result.speedMph()).isEqualByComparingTo(BigDecimal.valueOf(65));
        assertThat(result.engineRpm()).isEqualTo(2200);
        assertThat(result.coolantTempC()).isEqualTo(89);
        assertThat(result.vin()).isEqualTo("1FADP3L9XGL274054");
        assertThat(result.fuelLevelPercent()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(result.activeDtc()).isEqualTo("0:0");
        assertThat(result.batteryVoltage()).isEqualByComparingTo(BigDecimal.valueOf(12.45));
    }
}