package com.fleetwise.api.telematics.repository;

import com.fleetwise.api.telematics.entity.VehicleCurrentState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleCurrentStateRepository
        extends JpaRepository<VehicleCurrentState, UUID> {

    List<VehicleCurrentState> findByVehicleFleetId(UUID fleetId);
}