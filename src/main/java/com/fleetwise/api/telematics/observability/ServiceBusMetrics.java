package com.fleetwise.api.telematics.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ServiceBusMetrics {

    private final MeterRegistry registry;

    public ServiceBusMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void received() {
        registry.counter("trackora.servicebus.messages", "outcome", "received").increment();
    }

    public void completed() {
        registry.counter("trackora.servicebus.messages", "outcome", "completed").increment();
    }

    public void failed() {
        registry.counter("trackora.servicebus.messages", "outcome", "failed").increment();
    }
}