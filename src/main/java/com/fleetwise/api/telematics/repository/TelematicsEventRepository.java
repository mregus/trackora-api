package com.fleetwise.api.telematics.repository;

import com.fleetwise.api.telematics.entity.TelematicsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelematicsEventRepository
        extends JpaRepository<TelematicsEvent, UUID> {

    Optional<TelematicsEvent> findTopByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);
}