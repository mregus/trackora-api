package com.fleetwise.api.vehicle.service;

import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.fleet.service.FleetMemberService;
import com.fleetwise.api.vehicle.dto.*;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.entity.VehicleStatus;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final FleetRepository fleetRepository;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final FleetMemberService fleetMemberService;
    private final FleetAccessService fleetAccessService;

    @Transactional
    public VehicleResponse createVehicle(
            UUID fleetId,
            UUID ownerUserId,
            CreateVehicleRequest request
    ) {

        fleetMemberService.validateFleetAccess(
                fleetId,
                ownerUserId
        );

        fleetAccessService.validateWriteAccess(fleetId, ownerUserId);

        Fleet fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        Vehicle vehicle = Vehicle.builder()
                .fleet(fleet)
                .vin(request.vin())
                .make(request.make())
                .model(request.model())
                .year(request.year())
                .licensePlate(request.licensePlate())
                .currentMileage(request.currentMileage())
                .status(VehicleStatus.ACTIVE)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);

        User user = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        activityLogService.log(
                user,
                saved,
                ActivityAction.VEHICLE_UPDATED,
                "VEHICLE",
                saved.getId(),
                "Updated vehicle %s %s".formatted(saved.getMake(), saved.getModel())
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getFleetVehicles(
            UUID fleetId,
            UUID ownerUserId
    ) {

        fleetMemberService.validateFleetAccess(
                fleetId,
                ownerUserId
        );

        fleetAccessService.validateAccess(fleetId, ownerUserId);

        return vehicleRepository.findByFleetIdOrderByCreatedAtDesc(fleetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(
            UUID vehicleId,
            UUID ownerUserId
    ) {

        Vehicle vehicle = vehicleRepository.findByIdAndFleetOwnerId(
                        vehicleId,
                        ownerUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse updateVehicle(
            UUID vehicleId,
            UUID ownerUserId,
            UpdateVehicleRequest request
    ) {

        fleetAccessService.validateWriteAccess(request.fleetId(), ownerUserId);

        Vehicle vehicle = vehicleRepository.findByIdAndFleetOwnerId(
                        vehicleId,
                        ownerUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        vehicle.setVin(request.vin());
        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setCurrentMileage(request.currentMileage());
        vehicle.setStatus(request.status());

        if (request.fleetId() != null &&
                !request.fleetId().equals(vehicle.getFleet().getId())) {

            Fleet newFleet =
                    fleetRepository.findByIdAndOwnerId(
                            request.fleetId(),
                            ownerUserId
                    ).orElseThrow(() ->
                            new ResourceNotFoundException("Fleet not found"));

            vehicle.setFleet(newFleet);
        }

        return toResponse(vehicle);
    }

    @Transactional
    public void deleteVehicle(
            UUID vehicleId,
            UUID ownerUserId
    ) {

        Vehicle vehicle = vehicleRepository.findByIdAndFleetOwnerId(
                        vehicleId,
                        ownerUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateWriteAccess(vehicle.getFleet().getId(), ownerUserId);

        vehicleRepository.delete(vehicle);
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getFleet().getId(),
                vehicle.getVin(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getLicensePlate(),
                vehicle.getCurrentMileage(),
                vehicle.getStatus(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}