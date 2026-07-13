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

    Optional<Vehicle> findByIdAndFleetId(UUID id, UUID fleetId);
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

    @Query("""
    select vehicle
    from Vehicle vehicle
    where vehicle.fleet.id = :fleetId
      and (
          lower(vehicle.licensePlate) = lower(:query)
          or lower(vehicle.vin) = lower(:query)
          or lower(concat(vehicle.make, ' ', vehicle.model))
                like lower(concat('%', :query, '%'))
      )
    order by vehicle.make, vehicle.model
    """)
    List<Vehicle> findFleetVehiclesForCopilot(
            @Param("fleetId") UUID fleetId,
            @Param("query") String query
    );
}