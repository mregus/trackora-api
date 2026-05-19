package com.fleetwise.api.document.repository;

import com.fleetwise.api.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    List<VehicleDocument> findByVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

    Optional<VehicleDocument> findByIdAndVehicleFleetOwnerId(UUID id, UUID ownerId);

    List<VehicleDocument> findByVehicleIdAndMaintenanceIsNullOrderByCreatedAtDesc(UUID vehicleId);

    List<VehicleDocument> findByMaintenanceIdOrderByCreatedAtDesc(UUID maintenanceId);
}