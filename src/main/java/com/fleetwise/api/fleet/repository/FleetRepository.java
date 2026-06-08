package com.fleetwise.api.fleet.repository;

import com.fleetwise.api.fleet.entity.Fleet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FleetRepository extends JpaRepository<Fleet, UUID> {

    List<Fleet> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<Fleet> findByIdAndOwnerId(UUID fleetId, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID fleetId, UUID ownerId);

    Collection<Fleet> findByOwnerId(UUID ownerId);
}