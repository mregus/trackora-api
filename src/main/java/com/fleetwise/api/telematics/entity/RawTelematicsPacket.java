package com.fleetwise.api.telematics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "raw_telematics_packets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawTelematicsPacket {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "device_serial")
    private String deviceSerial;

    @Column(name = "packet_type")
    private String packetType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}