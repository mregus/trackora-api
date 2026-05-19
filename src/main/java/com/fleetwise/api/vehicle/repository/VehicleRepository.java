package com.fleetwise.api.vehicle.repository;

import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.entity.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByFleetIdOrderByCreatedAtDesc(UUID fleetId);

    Optional<Vehicle> findByIdAndFleetOwnerId(UUID vehicleId, UUID ownerUserId);

    long countByFleetId(UUID fleetId);

    long countByFleetIdAndStatus(UUID fleetId, VehicleStatus status);
}