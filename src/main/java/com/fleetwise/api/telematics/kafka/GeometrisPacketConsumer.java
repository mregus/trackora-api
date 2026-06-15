package com.fleetwise.api.telematics.kafka;

import com.fleetwise.api.telematics.service.TelematicsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "telematics.kafka.enabled",
        havingValue = "true"
)
public class GeometrisPacketConsumer {

    private final TelematicsService telematicsService;
    private final GeometrisFailedPacketProducer failedPacketProducer;

    @KafkaListener(
            topics = TelematicsTopics.GEOMETRIS_RAW_PACKETS,
            groupId = "trackora-telematics"
    )
    public void consume(String rawPacket) {
        try {
            log.info("Received Geometris packet from Kafka: {}", rawPacket);
            telematicsService.ingestGeometrisPacket(rawPacket);
        } catch (Exception ex) {
            log.error("Failed to process Geometris packet. Sending to DLQ.", ex);
            failedPacketProducer.publish(rawPacket);
        }
    }
}