package com.fleetwise.api.telematics.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "telematics.kafka.enabled",
        havingValue = "true"
)
public class GeometrisPacketProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String rawPacket) {
        String key = extractSerialNumber(rawPacket);

        kafkaTemplate.send(
                TelematicsTopics.DEVICE_TELEMETRY,
                key,
                rawPacket
        );
    }

    private String extractSerialNumber(String rawPacket) {
        if (rawPacket == null || rawPacket.isBlank()) {
            return "unknown";
        }

        String[] fields = rawPacket.split(",", -1);

        if (fields.length > 1 && fields[1] != null && !fields[1].isBlank()) {
            return fields[1].trim();
        }

        return "unknown";
    }
}