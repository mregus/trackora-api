package com.fleetwise.api.telematics.kafka;

import com.fleetwise.api.telematics.dto.LiveVehicleLocationEvent;
import com.fleetwise.api.telematics.entity.TelematicsEvent;
import com.fleetwise.api.telematics.service.TelematicsService;
import com.fleetwise.api.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "telematics.kafka.enabled",
        havingValue = "true"
)
public class DeviceTelemetryConsumer {

    private final TelematicsService telematicsService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;


    @KafkaListener(
            topics = TelematicsTopics.DEVICE_TELEMETRY,
            groupId = "trackora-device-telemetry"
    )
    public void consume(String rawPacket) {

        try {
            log.info("Received device telemetry packet: {}", rawPacket);

            telematicsService.ingestGeometrisPacket(rawPacket);

            TelematicsEvent saved = telematicsService.ingestGeometrisPacketEntity(rawPacket);

            Vehicle vehicle = saved.getVehicle();

            LiveVehicleLocationEvent event = new LiveVehicleLocationEvent(
                    vehicle.getId(),
                    vehicle.getFleet().getId(),
                    vehicle.getMake() + " " + vehicle.getModel(),
                    vehicle.getLicensePlate(),
                    saved.getLatitude(),
                    saved.getLongitude(),
                    saved.getSpeedMph(),
                    saved.getHeadingDegrees(),
                    saved.getFuelLevelPercent(),
                    saved.isCheckEngine(),
                    saved.getRecordedAt()
            );

            messagingTemplate.convertAndSend(
                    "/topic/fleets/" + vehicle.getFleet().getId(),
                    event
            );

        } catch (Exception ex) {
            String key = extractSerialNumber(rawPacket);
            log.error("Device {} telemetry processing failed. Sending to DLQ.", key, ex);

            kafkaTemplate.send(
                    TelematicsTopics.DEVICE_TELEMETRY_DLQ,
                    key,
                    rawPacket
            );
        }
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