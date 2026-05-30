package com.fleetwise.api.vehicle.repository;

import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByFleetIdOrderByCreatedAtDesc(UUID fleetId);

    Optional<Vehicle> findByIdAndFleetOwnerId(UUID vehicleId, UUID ownerUserId);

    long countByFleetId(UUID fleetId);

    long countByFleetIdAndStatus(UUID fleetId, VehicleStatus status);

    @Query("""
    select v
    from Vehicle v
    where v.fleet.owner.id = :ownerId
      and (
        lower(v.vin) like lower(concat('%', :query, '%'))
        or lower(v.licensePlate) like lower(concat('%', :query, '%'))
        or lower(v.make) like lower(concat('%', :query, '%'))
        or lower(v.model) like lower(concat('%', :query, '%'))
      )
    order by v.make asc, v.model asc
    """)
    List<Vehicle> searchVehicles(
            @Param("ownerId") UUID ownerId,
            @Param("query") String query
    );
}