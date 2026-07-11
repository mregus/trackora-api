package com.fleetwise.api.safety.entity;

import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "vehicle_safety_score_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_safety_score_history_day",
                        columnNames = {"vehicle_id", "score_date"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSafetyScoreHistory {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

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

    @Column(name = "miles_driven", precision = 12, scale = 2)
    private BigDecimal milesDriven;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}