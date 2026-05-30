package com.fleetwise.api.fuel.service;

import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fuel.dto.*;
import com.fleetwise.api.fuel.entity.FuelLog;
import com.fleetwise.api.fuel.repository.FuelLogRepository;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FuelLogService {

    private final FuelLogRepository fuelRepository;
    private final VehicleRepository vehicleRepository;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @Transactional
    public FuelLogResponse create(UUID vehicleId, UUID ownerId, CreateFuelLogRequest req) {
        var vehicle = vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        BigDecimal ppg = req.gallons().compareTo(BigDecimal.ZERO) > 0
                ? req.totalCost().divide(req.gallons(), 3, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        FuelLog log = FuelLog.builder()
                .vehicle(vehicle)
                .fuelDate(req.fuelDate())
                .mileage(req.mileage())
                .gallons(req.gallons())
                .totalCost(req.totalCost())
                .pricePerGallon(ppg)
                .notes(req.notes())
                .build();

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        activityLogService.log(
                user,
                vehicle,
                ActivityAction.FUEL_LOG_CREATED,
                "FUEL_LOG",
                log.getId(),
                "Added fuel log for %s %s".formatted(
                        vehicle.getMake(),
                        vehicle.getModel()
                )
        );

        return toResponse(fuelRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<FuelLogResponse> getVehicleLogs(UUID vehicleId, UUID ownerId) {
        vehicleRepository.findByIdAndFleetOwnerId(vehicleId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        return fuelRepository.findByVehicleIdOrderByFuelDateDesc(vehicleId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuelLogResponse> getFleetLogs(UUID fleetId, UUID ownerId) {
        var logs = fuelRepository.findByVehicleFleetOwnerId(ownerId);
        return logs.stream().filter(f ->
                f.getVehicle().getFleet().getId().equals(fleetId)
        ).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FuelLogResponse get(UUID id, UUID ownerId) {
        var log = fuelRepository.findByIdAndVehicleFleetOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel log not found"));
        return toResponse(log);
    }

    @Transactional
    public FuelLogResponse update(UUID id, UUID ownerId, UpdateFuelLogRequest req) {
        var log = fuelRepository.findByIdAndVehicleFleetOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel log not found"));
        log.setFuelDate(req.fuelDate());
        log.setMileage(req.mileage());
        log.setGallons(req.gallons());
        log.setTotalCost(req.totalCost());
        log.setPricePerGallon(req.totalCost().divide(req.gallons(), 3, java.math.RoundingMode.HALF_UP));
        log.setNotes(req.notes());
        return toResponse(log);
    }

    @Transactional
    public void delete(UUID id, UUID ownerId) {
        var log = fuelRepository.findByIdAndVehicleFleetOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel log not found"));
        fuelRepository.delete(log);
    }

    private FuelLogResponse toResponse(FuelLog f) {
        return new FuelLogResponse(
                f.getId(),
                f.getVehicle().getId(),
                f.getFuelDate(),
                f.getMileage(),
                f.getGallons(),
                f.getTotalCost(),
                f.getPricePerGallon(),
                f.getNotes(),
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}
