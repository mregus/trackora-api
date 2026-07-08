package com.fleetwise.api.safety.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle_safety_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSafetyScore {

    @Id
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    @Column(nullable = false)
    private int score;

    @Column(name = "hard_brakes", nullable = false)
    private int hardBrakes;

    @Column(name = "hard_accelerations", nullable = false)
    private int hardAccelerations;

    @Column(name = "harsh_turns", nullable = false)
    private int harshTurns;

    @Column(name = "speeding_events", nullable = false)
    private int speedingEvents;

    @Column(name = "idle_minutes", nullable = false)
    private int idleMinutes;

    @Column(name = "check_engine", nullable = false)
    private boolean checkEngine;

    @Column(name = "miles_driven")
    private BigDecimal milesDriven;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        } else {
            updatedAt = Instant.now();
        }
    }
}