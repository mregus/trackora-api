package com.fleetwise.api.telematics.observability;

import com.fleetwise.api.telematics.repository.RawTelematicsPacketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class GeometrisIngestionHealthIndicator implements HealthIndicator {

    private final RawTelematicsPacketRepository rawTelematicsPacketRepository;

    @Override
    public Health health() {
        return rawTelematicsPacketRepository
                .findTopByOrderByReceivedAtDesc()
                .map(packet -> Health.up()
                        .withDetail("lastPacketAt", packet.getReceivedAt())
                        .withDetail("lastDeviceSerial", packet.getDeviceSerial())
                        .withDetail("lastProcessed", packet.isProcessed())
                        .build())
                .orElseGet(() -> Health.unknown()
                        .withDetail("message", "No packets received yet")
                        .build());
    }
}