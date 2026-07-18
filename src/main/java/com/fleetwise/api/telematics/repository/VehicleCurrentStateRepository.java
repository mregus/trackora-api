package com.fleetwise.api.telematics.repository;

import com.fleetwise.api.telematics.entity.VehicleCurrentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VehicleCurrentStateRepository
        extends JpaRepository<VehicleCurrentState, UUID> {

    List<VehicleCurrentState> findByVehicleFleetId(UUID fleetId);

    List<VehicleCurrentState> findByLastSeenAtBefore(Instant cutoff);

    @Query("""
    select state
    from VehicleCurrentState state
    join fetch state.vehicle
    where state.lastSeenAt < :cutoff
    """)
    List<VehicleCurrentState> findStaleStatesWithVehicle(
            @Param("cutoff") Instant cutoff
    );
}