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
        kafkaTemplate.send(
                TelematicsTopics.GEOMETRIS_RAW_PACKETS,
                rawPacket
        );
    }
}