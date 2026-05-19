package com.fleetwise.api.ai.entity;

import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_insights")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AiInsight {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_id", nullable = false)
    private Fleet fleet;

    @Column(name = "prompt_hash")
    private String promptHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @PrePersist
    void prePersist() {
        id = id == null ? UUID.randomUUID() : id;
        createdAt = Instant.now();
    }
}
