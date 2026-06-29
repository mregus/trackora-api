package com.fleetwise.api.telematics.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeometrisServiceBusSender {

    private final ServiceBusSenderClient sender;

    public GeometrisServiceBusSender(
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.queue-name}") String queueName) {

        this.sender =
                new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .sender()
                        .queueName(queueName)
                        .buildClient();
    }

    public void send(String packet) {
        sender.sendMessage(new ServiceBusMessage(packet));
    }
}