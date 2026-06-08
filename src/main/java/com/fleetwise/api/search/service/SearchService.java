package com.fleetwise.api.search.service;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.search.dto.AlertSearchResult;
import com.fleetwise.api.search.dto.MaintenanceSearchResult;
import com.fleetwise.api.search.dto.SearchResponse;
import com.fleetwise.api.search.dto.VehicleSearchResult;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final AlertRepository alertRepository;
    private final FleetAccessService fleetAccessService;

    @Transactional(readOnly = true)
    public SearchResponse search(UUID ownerId, String query) {
        String normalizedQuery = query == null ? "" : query.trim();

        if (normalizedQuery.length() < 2) {
            return new SearchResponse(
                    java.util.List.of(),
                    java.util.List.of(),
                    java.util.List.of()
            );
        }

        var vehicles = vehicleRepository.searchVehicles(ownerId, normalizedQuery)
                .stream()
                .limit(10)
                .map(this::toVehicleResult)
                .toList();

        fleetAccessService.validateAccess(vehicles.getFirst().fleetId(), ownerId);

        var maintenance = maintenanceRepository.searchMaintenance(ownerId, normalizedQuery)
                .stream()
                .limit(10)
                .map(this::toMaintenanceResult)
                .toList();

        var alerts = alertRepository.searchAlerts(ownerId, normalizedQuery)
                .stream()
                .limit(10)
                .map(this::toAlertResult)
                .toList();

        return new SearchResponse(vehicles, maintenance, alerts);
    }

    private VehicleSearchResult toVehicleResult(Vehicle vehicle) {
        String label = "%s %s %s".formatted(
                vehicle.getYear(),
                vehicle.getMake(),
                vehicle.getModel()
        );

        return new VehicleSearchResult(
                vehicle.getId(),
                vehicle.getFleet().getId(),
                label,
                vehicle.getVin(),
                vehicle.getLicensePlate(),
                vehicle.getYear(),
                vehicle.getMake(),
                vehicle.getModel()
        );
    }

    private MaintenanceSearchResult toMaintenanceResult(Maintenance maintenance) {
        var vehicle = maintenance.getVehicle();

        String label = "%s - %s %s".formatted(
                maintenance.getServiceType(),
                vehicle.getMake(),
                vehicle.getModel()
        );

        return new MaintenanceSearchResult(
                maintenance.getId(),
                vehicle.getId(),
                vehicle.getFleet().getId(),
                label,
                maintenance.getServiceType(),
                maintenance.getStatus().name(),
                maintenance.getServiceDate()
        );
    }

    private AlertSearchResult toAlertResult(Alert alert) {
        String label = "%s - %s".formatted(
                alert.getSeverity(),
                alert.getType()
        );

        return new AlertSearchResult(
                alert.getId(),
                alert.getFleet().getId(),
                alert.getVehicle() != null ? alert.getVehicle().getId() : null,
                label,
                alert.getType().name(),
                alert.getSeverity().name(),
                alert.isResolved(),
                alert.getCreatedAt()
        );
    }
}