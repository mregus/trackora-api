package com.fleetwise.api.telematics.geometris;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class GeometrisGpsTrailDecoder {

    public List<GeometrisGpsTrailPoint> decode(
            Double baseLatitude,
            Double baseLongitude,
            Instant baseRecordedAt,
            String locationTrail
    ) {
        if (baseLatitude == null ||
                baseLongitude == null ||
                baseRecordedAt == null ||
                locationTrail == null ||
                locationTrail.isBlank()) {
            return List.of();
        }

        List<GeometrisGpsTrailPoint> points = new ArrayList<>();

        double currentLat = baseLatitude;
        double currentLon = baseLongitude;
        Instant currentTime = baseRecordedAt;

        String[] trailPoints = locationTrail.split("\\.");

        for (String trailPoint : trailPoints) {
            if (trailPoint == null || trailPoint.isBlank()) {
                continue;
            }

            String[] parts = trailPoint.split(":");

            if (parts.length < 4) {
                continue;
            }

            int latDiff = Integer.parseInt(parts[0]);
            int lonDiff = Integer.parseInt(parts[1]);
            BigDecimal speedMph = new BigDecimal(parts[2]);
            int timeDiffSeconds = Integer.parseInt(parts[3]);

            currentLat = currentLat - (latDiff / 1_000_000.0);
            currentLon = currentLon - (lonDiff / 1_000_000.0);
            currentTime = currentTime.minusSeconds(timeDiffSeconds);

            points.add(new GeometrisGpsTrailPoint(
                    currentLat,
                    currentLon,
                    speedMph,
                    currentTime
            ));
        }

        return points;
    }
}