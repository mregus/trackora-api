package com.fleetwise.api.safety.repository;

import com.fleetwise.api.safety.entity.VehicleSafetyScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleSafetyScoreRepository extends JpaRepository<VehicleSafetyScore, UUID> {

    Optional<VehicleSafetyScore> findByVehicleId(UUID vehicleId);

    List<VehicleSafetyScore> findByVehicleFleetIdOrderByScoreDesc(UUID fleetId);

    @Query("""
    select s
    from VehicleSafetyScore s
    join fetch s.vehicle v
    where v.fleet.id = :fleetId
    order by s.score desc
    """)
    List<VehicleSafetyScore> findScoresByFleetId(UUID fleetId);
}