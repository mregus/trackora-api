package com.fleetwise.api.fuel.repository;

import com.fleetwise.api.fuel.entity.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public interface FuelLogRepository extends JpaRepository<FuelLog, UUID> {
    List<FuelLog> findByVehicleIdOrderByFuelDateDesc(UUID vehicleId);
    List<FuelLog> findByVehicleFleetOwnerId(UUID ownerId);
    Optional<FuelLog> findByIdAndVehicleFleetOwnerId(UUID id, UUID ownerId);
    @Query("""
    select coalesce(sum(f.totalCost), 0)
    from FuelLog f
    where f.vehicle.fleet.id = :fleetId
      and f.fuelDate >= :startDate
    """)
    BigDecimal sumCostByFleetIdSince(
            @Param("fleetId") UUID fleetId,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
    select distinct f.vehicle.fleet.id
    from FuelLog f
    """)
    List<UUID> findFleetIdsWithFuelLogs();

    List<FuelLog> findByVehicleFleetId(UUID fleetId);
}
