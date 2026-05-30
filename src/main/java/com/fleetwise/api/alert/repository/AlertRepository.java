package com.fleetwise.api.alert.repository;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByFleetIdOrderByCreatedAtDesc(UUID fleetId);
    Optional<Alert> findByIdAndFleetOwnerId(UUID id, UUID ownerUserId);
    long countByFleetIdAndResolvedFalse(UUID fleetId);

    boolean existsByVehicleIdAndTypeAndResolvedFalse(
            UUID vehicleId,
            AlertType type
    );

    @Query("""
    select a
    from Alert a
    left join fetch a.vehicle v
    join fetch a.fleet f
    where f.owner.id = :ownerId
      and (
        lower(cast(a.type as string)) like lower(concat('%', :query, '%'))
        or lower(cast(a.severity as string)) like lower(concat('%', :query, '%'))
        or lower(a.message) like lower(concat('%', :query, '%'))
        or lower(coalesce(v.make, '')) like lower(concat('%', :query, '%'))
        or lower(coalesce(v.model, '')) like lower(concat('%', :query, '%'))
        or lower(coalesce(v.licensePlate, '')) like lower(concat('%', :query, '%'))
      )
    order by a.createdAt desc
    """)
    List<Alert> searchAlerts(
            @Param("ownerId") UUID ownerId,
            @Param("query") String query
    );

//    boolean existsByVehicleIdAndTypeAndResolvedFalse(UUID vehicleId, String type);
}
