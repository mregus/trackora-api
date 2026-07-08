package com.fleetwise.api.telematics.observability.service;

import com.fleetwise.api.telematics.observability.dto.ObservabilityStatusResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObservabilityStatusService {

    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    public ObservabilityStatusResponse getStatus() {
        return new ObservabilityStatusResponse(
                healthEndpoint.health().getStatus().getCode(),
                value("trackora.telematics.packets.received"),
                value("trackora.telematics.packets.processed"),
                value("trackora.telematics.packets.failed"),
                value("trackora.servicebus.messages", "outcome", "received"),
                value("trackora.servicebus.messages", "outcome", "completed"),
                value("trackora.servicebus.messages", "outcome", "failed"),
                value("trackora.servicebus.dlq.depth"),
                value("trackora.vehicles.online"),
                value("trackora.vehicles.stale"),
                value("trackora.vehicles.offline"),
                value("trackora.alerts.active")
        );
    }

    private double value(String metricName) {
        var counter = meterRegistry.find(metricName).counter();
        if (counter != null) {
            return counter.count();
        }

        var gauge = meterRegistry.find(metricName).gauge();
        if (gauge != null) {
            return gauge.value();
        }

        return 0;
    }

    private double value(String metricName, String tag, String tagValue) {
        var counter = meterRegistry.find(metricName)
                .tag(tag, tagValue)
                .counter();

        return counter == null ? 0 : counter.count();
    }
}