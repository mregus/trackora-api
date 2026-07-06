package com.fleetwise.api.telematics.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ServiceBusDlqMetrics {

    private final MeterRegistry registry;

    public ServiceBusDlqMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void peeked(int count) {
        registry.counter("trackora.servicebus.dlq.messages.peeked")
                .increment(count);
    }

    public void replayed() {
        registry.counter("trackora.servicebus.dlq.messages.replayed")
                .increment();
    }

    public void deleted() {
        registry.counter("trackora.servicebus.dlq.messages.deleted")
                .increment();
    }

    public void replayFailed() {
        registry.counter("trackora.servicebus.dlq.messages.replay_failed")
                .increment();
    }
}