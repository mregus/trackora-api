package com.fleetwise.api.telematics.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.fleetwise.api.telematics.observability.ServiceBusMetrics;
import com.fleetwise.api.telematics.observability.TelemetrySource;
import com.fleetwise.api.telematics.service.TelematicsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "azure.servicebus.enabled",
        havingValue = "true"
)
public class GeometrisServiceBusConsumer {

    private final ServiceBusProcessorClient processorClient;

    public GeometrisServiceBusConsumer(
            TelematicsService telematicsService,
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.queue-name}") String queueName, ServiceBusMetrics serviceBusMetrics) {

        this.processorClient =
                new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .processor()
                        .queueName(queueName)
                        .processMessage(context -> {
                            String rawPacket = context.getMessage().getBody().toString();

                            log.info("Received Geometris packet from Azure Service Bus: {}", rawPacket);

                            serviceBusMetrics.received();

                            log.info(
                                    "Service Bus message received messageId={}, deliveryCount={}, sequenceNumber={}, enqueuedTime={}",
                                    context.getMessage().getMessageId(),
                                    context.getMessage().getDeliveryCount(),
                                    context.getMessage().getSequenceNumber(),
                                    context.getMessage().getEnqueuedTime()
                            );

                            try {
                                telematicsService.ingestGeometrisPacketEntity(
                                        rawPacket,
                                        TelemetrySource.AZURE_SERVICE_BUS
                                );

                                serviceBusMetrics.completed();

                            } catch (Exception ex) {
                                serviceBusMetrics.failed();
                                throw ex;
                            }
                        })
                        .processError(error -> {
                            log.error("Azure Service Bus processing error", error.getException());
                        })
                        .buildProcessorClient();
    }

    @PostConstruct
    public void start() {
        processorClient.start();
    }

    @PreDestroy
    public void stop() {
        processorClient.close();
    }
}