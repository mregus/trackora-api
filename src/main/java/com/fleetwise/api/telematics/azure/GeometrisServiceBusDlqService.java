package com.fleetwise.api.telematics.azure;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.SubQueue;
import com.fleetwise.api.telematics.observability.ServiceBusDlqMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@ConditionalOnProperty(
        name = "azure.servicebus.enabled",
        havingValue = "true"
)
public class GeometrisServiceBusDlqService {

    private final ServiceBusReceiverClient dlqReceiver;
    private final ServiceBusSenderClient mainQueueSender;
    private final ServiceBusDlqMetrics dlqMetrics;

    public GeometrisServiceBusDlqService(
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.queue-name}") String queueName,
            ServiceBusDlqMetrics dlqMetrics
    ) {
        this.dlqMetrics = dlqMetrics;

        this.dlqReceiver = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .buildClient();

        this.mainQueueSender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

    public List<ServiceBusReceivedMessage> peekDeadLetters(int maxMessages) {
        IterableStream<ServiceBusReceivedMessage> messages =
                dlqReceiver.peekMessages(maxMessages);

        List<ServiceBusReceivedMessage> result =
                StreamSupport.stream(messages.spliterator(), false)
                        .toList();

        dlqMetrics.peeked(result.size());

        return result;
    }

    public void deleteDeadLetter(String messageId) {
        ServiceBusReceivedMessage message = findByMessageId(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Dead letter message not found"));

        dlqReceiver.complete(message);

        dlqMetrics.deleted();
    }

    public void replayDeadLetter(String messageId) {
        ServiceBusReceivedMessage message = findByMessageId(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Dead letter message not found"));

        String body = message.getBody().toString();

        if (body.trim().startsWith("{")) {
            dlqMetrics.replayFailed();
            throw new IllegalStateException("JSON dead-letter messages cannot be replayed into the CSV parser yet.");
        }

        mainQueueSender.sendMessage(new ServiceBusMessage(body));

        dlqReceiver.complete(message);

        dlqMetrics.replayed();
    }

    private Optional<ServiceBusReceivedMessage> findByMessageId(String messageId) {
        List<ServiceBusReceivedMessage> messages =
                StreamSupport.stream(dlqReceiver.receiveMessages(50).spliterator(), false)
                        .toList();

        return messages.stream()
                .filter(message -> messageId.equals(message.getMessageId()))
                .findFirst();
    }
}