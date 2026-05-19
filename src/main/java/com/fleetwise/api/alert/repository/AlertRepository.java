package com.fleetwise.api.alert.repository;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByFleetIdOrderByCreatedAtDesc(UUID fleetId);
    Optional<Alert> findByIdAndFleetOwnerId(UUID id, UUID ownerUserId);
    long countByFleetIdAndResolvedFalse(UUID fleetId);

    boolean existsByVehicleIdAndTypeAndResolvedFalse(
            UUID vehicleId,
            AlertType type
    );

//    boolean existsByVehicleIdAndTypeAndResolvedFalse(UUID vehicleId, String type);
}
