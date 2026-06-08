package com.fleetwise.api.fleet.repository;

import com.fleetwise.api.fleet.entity.FleetMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FleetMemberRepository extends JpaRepository<FleetMember, UUID> {

    List<FleetMember> findByFleetIdOrderByCreatedAtAsc(UUID fleetId);

    Optional<FleetMember> findByFleetIdAndUserId(UUID fleetId, UUID userId);

    boolean existsByFleetIdAndUserId(UUID fleetId, UUID userId);

    @Query("""
    select fm.fleet.id
    from FleetMember fm
    where fm.user.id = :userId
    """)
    List<UUID> findFleetIdsByUserId(UUID userId);
}