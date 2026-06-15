package com.fleetwise.api.telematics.geometris;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "geometris_raw_packets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeometrisRawPacket {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "reason_text")
    private String reasonText;

    @Column(name = "raw_packet", nullable = false, columnDefinition = "TEXT")
    private String rawPacket;

    @Column(name = "parsed_successfully", nullable = false)
    private boolean parsedSuccessfully;

    @Column(name = "error_message", columnDefinition = "TEXT")
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