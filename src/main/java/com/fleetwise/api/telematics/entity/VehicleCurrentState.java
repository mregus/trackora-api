package com.fleetwise.api.telematics.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle_current_state")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCurrentState {

    @Id
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private Double latitude;
    private Double longitude;

    @Column(name = "ignition_on")
    private Boolean ignitionOn;

    @Column(name = "idle_started_at")
    private Instant idleStartedAt;

    @Column(name = "current_idle_minutes", nullable = false)
    private Integer currentIdleMinutes = 0;

    @Column(name = "speed_mph")
    private BigDecimal speedMph;

    @Column(name = "fuel_level_percent")
    private BigDecimal fuelLevelPercent;

    @Column(name = "heading_degrees")
    private Integer headingDegrees;

    @Column(name = "check_engine", nullable = false)
    private boolean checkEngine;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}