package com.fleetwise.api.telematics.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telematics_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelematicsEvent {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    private Double latitude;
    private Double longitude;

    @Column(name = "speed_mph")
    private BigDecimal speedMph;

    @Column(name = "odometer_miles")
    private BigDecimal odometerMiles;

    @Column(name = "fuel_level_percent")
    private BigDecimal fuelLevelPercent;

    @Column(name = "engine_temp_f")
    private BigDecimal engineTempF;

    @Column(name = "battery_voltage")
    private BigDecimal batteryVoltage;

    @Column(name = "check_engine", nullable = false)
    private boolean checkEngine;

    @Column(name = "heading_degrees")
    private Integer headingDegrees;

    @Column(name = "harsh_braking", nullable = false)
    private boolean harshBraking;

    @Column(name = "idle_minutes", nullable = false)
    private Integer idleMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (recordedAt == null) {
            recordedAt = Instant.now();
        }

        if (idleMinutes == null) {
            idleMinutes = 0;
        }
    }
}