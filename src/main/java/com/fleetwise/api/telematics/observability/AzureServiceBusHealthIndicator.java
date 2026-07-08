package com.fleetwise.api.telematics.observability;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "azure.servicebus.enabled", havingValue = "true")
public class AzureServiceBusHealthIndicator implements HealthIndicator {

    private final ServiceBusReceiverClient receiver;

    public AzureServiceBusHealthIndicator(
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.queue-name}") String queueName
    ) {
        this.receiver = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .buildClient();
    }

    @Override
    public Health health() {
        try {
            receiver.peekMessage();

            return Health.up()
                    .withDetail("queue", "reachable")
                    .build();

        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("queue", "unreachable")
                    .build();
        }
    }
}