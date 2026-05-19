package com.fleetwise.api.alert.entity;

import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Alert {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_id", nullable = false)
    private Fleet fleet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean resolved = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    private Instant createdAt;
    private Instant resolvedAt;

    @PrePersist
    void prePersist() {
        id = id == null ? UUID.randomUUID() : id;
        createdAt = Instant.now();
    }
}
