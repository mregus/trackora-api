package com.fleetwise.api.telematics.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class DlqReplayService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;

    public int replayDeviceTelemetryDlq(int maxMessages) {
        int limit = maxMessages <= 0 ? 10 : maxMessages;
        AtomicInteger replayed = new AtomicInteger();

        var consumer = consumerFactory.createConsumer(
                "trackora-dlq-replay-" + UUID.randomUUID(),
                null
        );

        try {
            consumer.subscribe(List.of(TelematicsTopics.DEVICE_TELEMETRY_DLQ));

            long deadline = System.currentTimeMillis() + 10_000;

            while (System.currentTimeMillis() < deadline && replayed.get() < limit) {
                var records = consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, String> record : records) {
                    if (replayed.get() >= limit) {
                        break;
                    }

                    kafkaTemplate.send(
                            TelematicsTopics.DEVICE_TELEMETRY,
                            record.key(),
                            record.value()
                    );

                    replayed.incrementAndGet();
                }
            }

            consumer.commitSync();
            return replayed.get();

        } finally {
            consumer.close();
        }
    }
}