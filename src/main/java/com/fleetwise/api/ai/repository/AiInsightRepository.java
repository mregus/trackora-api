package com.fleetwise.api.ai.repository;

import com.fleetwise.api.ai.entity.AiInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.*;

public interface AiInsightRepository extends JpaRepository<AiInsight, UUID> {
    boolean existsByFleetIdAndVehicleIsNullAndCreatedAtAfter(
            UUID fleetId,
            Instant createdAt
    );

    boolean existsByVehicleIdAndCreatedAtAfter(
            UUID vehicleId,
            Instant createdAt
    );
    List<AiInsight> findByFleetIdOrderByCreatedAtDesc(UUID fleetId);
    Optional<AiInsight> findFirstByFleetIdOrderByCreatedAtDesc(UUID fleetId);

    Optional<AiInsight> findFirstByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

    List<AiInsight> findByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

    Optional<AiInsight> findFirstByFleetIdAndVehicleIsNullOrderByCreatedAtDesc(UUID fleetId);

    List<AiInsight> findByFleetIdAndVehicleIsNullOrderByCreatedAtDesc(UUID fleetId);
}
