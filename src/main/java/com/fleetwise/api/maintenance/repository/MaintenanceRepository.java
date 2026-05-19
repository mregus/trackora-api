package com.fleetwise.api.maintenance.repository;

import com.fleetwise.api.fuel.entity.FuelLog;
import com.fleetwise.api.maintenance.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {
    List<Maintenance> findByVehicleIdOrderByServiceDateDesc(UUID vehicleId);
    Optional<Maintenance> findByIdAndVehicleFleetOwnerId(UUID id, UUID ownerUserId);
    @Query("""
    select coalesce(sum(m.cost), 0)
    from Maintenance m
    where m.vehicle.fleet.id = :fleetId
      and m.serviceDate >= :startDate
    """)
    BigDecimal sumCostByFleetIdSince(
            @Param("fleetId") UUID fleetId,
            @Param("startDate") LocalDate startDate
    );

    @Query("""
    select m
    from Maintenance m
    where m.status = 'SCHEDULED'
      and m.serviceDate <= :dueDate
      and (
        m.mileage is null
        or m.vehicle.currentMileage < m.mileage
      )
    """)
    List<Maintenance> findScheduledMaintenanceDueBy(
            @Param("dueDate") LocalDate dueDate
    );

    @Query("""
    select m
    from Maintenance m
    where m.status = 'SCHEDULED'
      and m.mileage is not null
      and m.vehicle.currentMileage >= m.mileage
    """)
    List<Maintenance> findScheduledMaintenanceOverdueByMileage();

    @Query("""
    select avg(f.pricePerGallon)
    from FuelLog f
    where f.vehicle.fleet.id = :fleetId
    """)
    Double findAveragePricePerGallonByFleetId(
            @Param("fleetId") UUID fleetId
    );

    @Query("""
    select f
    from FuelLog f
    where f.pricePerGallon > :threshold
    """)
    List<FuelLog> findFuelLogsAboveThreshold(
            @Param("threshold") BigDecimal threshold
    );
}
