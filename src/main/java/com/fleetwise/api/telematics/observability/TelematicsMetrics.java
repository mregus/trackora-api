package com.fleetwise.api.telematics.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class TelematicsMetrics {

    private final MeterRegistry registry;

    @Getter
    private final Timer parseTimer;
    @Getter
    private final Timer rawPacketSaveTimer;
    @Getter
    private final Timer deviceLookupTimer;
    @Getter
    private final Timer eventSaveTimer;
    @Getter
    private final Timer currentStateTimer;
    @Getter
    private final Timer alertGenerationTimer;
    @Getter
    private final Timer websocketTimer;

    public TelematicsMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.parseTimer = Timer.builder("trackora.telematics.stage.parse")
                .description("Packet parsing duration")
                .register(registry);

        this.rawPacketSaveTimer = Timer.builder("trackora.telematics.stage.raw-save")
                .description("Raw packet persistence duration")
                .register(registry);

        this.deviceLookupTimer = Timer.builder("trackora.telematics.stage.device-lookup")
                .description("Telematics device lookup duration")
                .register(registry);

        this.eventSaveTimer = Timer.builder("trackora.telematics.stage.event-save")
                .description("Telematics event persistence duration")
                .register(registry);

        this.currentStateTimer = Timer.builder("trackora.telematics.stage.current-state")
                .description("Vehicle current state update duration")
                .register(registry);

        this.alertGenerationTimer = Timer.builder("trackora.telematics.stage.alerts")
                .description("Alert generation duration")
                .register(registry);

        this.websocketTimer = Timer.builder("trackora.telematics.stage.websocket")
                .description("WebSocket publish duration")
                .register(registry);
    }

    public void packetReceived(TelemetrySource source) {
        registry.counter(
                "trackora.telematics.packets.received",
                "source", source.name()
        ).increment();
    }

    public void packetProcessed(TelemetrySource source) {
        registry.counter(
                "trackora.telematics.packets",
                "source", source.name(),
                "outcome", "processed"
        ).increment();
    }

    public void packetType(
            TelemetrySource source,
            String format,
            String reason
    ) {
        registry.counter(
                "trackora.telematics.packet.types",
                "source", source.name(),
                "format", format == null ? "UNKNOWN" : format,
                "reason", reason == null ? "UNKNOWN" : reason
        ).increment();
    }

    public void packetFailed(
            TelemetrySource source,
            TelemetryFailureReason reason
    ) {
        registry.counter(
                "trackora.telematics.packets",
                "source", source.name(),
                "outcome", "failed",
                "reason", reason.name()
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

    public <T> T record(Timer timer, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(registry);
        try {
            return supplier.get();
        } finally {
            sample.stop(timer);
        }
    }
}