package com.fleetwise.api.telematics.geometris;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GeometrisGpsTrailDecoderTest {

    private final GeometrisGpsTrailDecoder decoder =
            new GeometrisGpsTrailDecoder();

    @Test
    void decode_ShouldDecodeGpsTrailPoints() {
        String trail =
                "-631:607:25:6.-359:383:4:9";

        var points = decoder.decode(
                38.346620,
                -77.500258,
                Instant.ofEpochSecond(1665570886),
                trail
        );

        assertThat(points).hasSize(2);

        assertThat(points.get(0).latitude())
                .isEqualTo(38.347251);

        assertThat(points.get(0).longitude())
                .isEqualTo(-77.500865);

        assertThat(points.get(0).speedMph())
                .isEqualByComparingTo("25");

        assertThat(points.get(0).recordedAt())
                .isEqualTo(Instant.ofEpochSecond(1665570880));
    }
}