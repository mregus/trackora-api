package com.fleetwise.api.telematics.observability;

import com.fleetwise.api.telematics.azure.GeometrisServiceBusDlqService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "azure.servicebus.enabled",
        havingValue = "true"
)
public class ServiceBusDlqGaugeMetrics {

    public ServiceBusDlqGaugeMetrics(
            MeterRegistry registry,
            GeometrisServiceBusDlqService dlqService
    ) {
        Gauge.builder("trackora.servicebus.dlq.depth", dlqService, GeometrisServiceBusDlqService::getDeadLetterCount)
                .description("Current Azure Service Bus dead-letter queue depth")
                .register(registry);
    }
}