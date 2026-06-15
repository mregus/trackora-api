package com.fleetwise.api.telematics.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telematics_devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelematicsDevice {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TelematicsProvider provider;

    @Column(name = "external_device_id", nullable = false)
    private String externalDeviceId;

    @Column(name = "serial_number")
    private String serialNumber;

    private String imei;

    private String vin;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
        active = true;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}