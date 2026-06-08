package com.fleetwise.api.fleet.repository;

import com.fleetwise.api.fleet.entity.FleetInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FleetInvitationRepository
        extends JpaRepository<FleetInvitation, UUID> {

    Optional<FleetInvitation> findByToken(String token);

    List<FleetInvitation> findByFleetId(UUID fleetId);

    boolean existsByFleetIdAndEmailAndAcceptedFalse(
            UUID fleetId,
            String email
    );

    Optional<FleetInvitation> findByIdAndFleetId(UUID id, UUID fleetId);
}