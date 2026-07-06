package com.fleetwise.api.telematics.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class TelematicsMetrics {

    private final MeterRegistry registry;

    public TelematicsMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void packetReceived(TelemetrySource source) {
        registry.counter(
                "trackora.telematics.packets.received",
                "source", source.name()
        ).increment();
    }

    public void packetProcessed(TelemetrySource source) {
        registry.counter(
                "trackora.telematics.packets.processed",
                "source", source.name()
        ).increment();
    }

    public void packetFailed(TelemetrySource source) {
        registry.counter(
                "trackora.telematics.packets.failed",
                "source", source.name()
        ).increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample, TelemetrySource source) {
        sample.stop(
                registry.timer(
                        "trackora.telematics.processing.duration",
                        "source", source.name()
                )
        );
    }
}