package com.fleetwise.api.maintenance.service;

import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import com.fleetwise.api.maintenance.dto.*;
import com.fleetwise.api.maintenance.entity.*;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final VehicleRepository vehicleRepository;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final FleetAccessService fleetAccessService;

    @Transactional
    public MaintenanceResponse create(UUID vehicleId, UUID ownerId, CreateMaintenanceRequest request) {

        var vehicle = vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateWriteAccess(vehicle.getFleet().getId(), ownerId);

        Maintenance record = Maintenance.builder()
                .vehicle(vehicle)
                .serviceType(request.serviceType())
                .description(request.description())
                .serviceDate(request.serviceDate())
                .mileage(request.mileage())
                .cost(request.cost() == null ? BigDecimal.ZERO : request.cost())
                .vendor(request.vendor())
                .nextServiceDate(request.nextServiceDate())
                .status(MaintenanceStatus.SCHEDULED)
                .build();

        Maintenance saved = maintenanceRepository.save(record);

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        activityLogService.log(
                user,
                vehicle,
                ActivityAction.MAINTENANCE_CREATED,
                "MAINTENANCE",
                saved.getId(),
                "Scheduled maintenance %s for %s %s".formatted(
                        saved.getServiceType(),
                        vehicle.getMake(),
                        vehicle.getModel()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getVehicleRecords(UUID vehicleId, UUID ownerId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), ownerId);

        vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        return maintenanceRepository.findByVehicleIdOrderByServiceDateDesc(vehicleId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getRecord(UUID id, UUID ownerId) {

        var record = maintenanceRepository.findByIdAndVehicleFleetOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found"));

        Vehicle vehicle = vehicleRepository.findById(record.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), ownerId);

        return toResponse(record);
    }

    @Transactional
    public MaintenanceResponse update(UUID id, UUID ownerId, UpdateMaintenanceRequest req) {

        var rec = maintenanceRepository.findByIdAndVehicleFleetOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found"));

        fleetAccessService.validateWriteAccess(rec.getVehicle().getFleet().getId(), ownerId);

        rec.setServiceType(req.serviceType());
        rec.setDescription(req.description());
        rec.setServiceDate(req.serviceDate());
        rec.setMileage(req.mileage());
        rec.setCost(req.cost());
        rec.setVendor(req.vendor());
        rec.setNextServiceDate(req.nextServiceDate());
        rec.setStatus(req.status());

        MaintenanceStatus oldStatus = rec.getStatus();

        rec.setStatus(req.status());

        if (oldStatus != MaintenanceStatus.COMPLETED
                && req.status() == MaintenanceStatus.COMPLETED) {

            User user = userRepository.findById(ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            activityLogService.log(
                    user,
                    rec.getVehicle(),
                    ActivityAction.MAINTENANCE_COMPLETED,
                    "MAINTENANCE",
                    rec.getId(),
                    "Completed maintenance %s for %s %s".formatted(
                            rec.getServiceType(),
                            rec.getVehicle().getMake(),
                            rec.getVehicle().getModel()
                    )
            );
        }

        return toResponse(rec);
    }

    @Transactional
    public void delete(UUID id, UUID ownerId) {
        var rec = maintenanceRepository.findByIdAndVehicleFleetOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found"));

        fleetAccessService.validateWriteAccess(rec.getVehicle().getFleet().getId(), ownerId);

        maintenanceRepository.delete(rec);
    }

    private MaintenanceResponse toResponse(Maintenance m) {
        return new MaintenanceResponse(
                m.getId(),
                m.getVehicle().getId(),
                m.getServiceType(),
                m.getDescription(),
                m.getServiceDate(),
                m.getMileage(),
                m.getCost(),
                m.getVendor(),
                m.getNextServiceDate(),
                m.getStatus(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
