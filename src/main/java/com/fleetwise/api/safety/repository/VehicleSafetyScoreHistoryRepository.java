package com.fleetwise.api.safety.repository;

import com.fleetwise.api.safety.entity.VehicleSafetyScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleSafetyScoreHistoryRepository
        extends JpaRepository<VehicleSafetyScoreHistory, UUID> {

    Optional<VehicleSafetyScoreHistory> findByVehicleIdAndScoreDate(
            UUID vehicleId,
            LocalDate scoreDate
    );

    List<VehicleSafetyScoreHistory>
    findByVehicleFleetIdAndScoreDateBetweenOrderByScoreDateAsc(
            UUID fleetId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<VehicleSafetyScoreHistory>
    findByVehicleIdAndScoreDateBetweenOrderByScoreDateAsc(
            UUID vehicleId,
            LocalDate startDate,
            LocalDate endDate
    );
}