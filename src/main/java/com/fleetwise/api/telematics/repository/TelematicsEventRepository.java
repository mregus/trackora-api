package com.fleetwise.api.telematics.repository;

import com.fleetwise.api.telematics.entity.TelematicsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelematicsEventRepository
        extends JpaRepository<TelematicsEvent, UUID> {

    Optional<TelematicsEvent> findTopByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);

    @Query("""
    select t
    from TelematicsEvent t
    where t.vehicle.fleet.id = :fleetId
      and t.recordedAt = (
          select max(t2.recordedAt)
          from TelematicsEvent t2
          where t2.vehicle.id = t.vehicle.id
      )
    """)
    List<TelematicsEvent> findLatestByFleetId(UUID fleetId);


    @Query("""
    select t
    from TelematicsEvent t
    where t.vehicle.id = :vehicleId
      and t.latitude is not null
      and t.longitude is not null
      and t.recordedAt between :start and :end
    order by t.recordedAt asc
    """)
    List<TelematicsEvent> findHistoryByVehicleId(
            UUID vehicleId,
            Instant start,
            Instant end
    );
}