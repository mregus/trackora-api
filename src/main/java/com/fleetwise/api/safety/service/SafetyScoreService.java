package com.fleetwise.api.safety.service;

import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
import com.fleetwise.api.safety.entity.VehicleSafetyScore;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SafetyScoreService {

    private final FleetAccessService fleetAccessService;
    private final VehicleSafetyScoreRepository vehicleSafetyScoreRepository;

    public List<VehicleSafetyScoreResponse> getFleetSafetyScores(
            UUID userId,
            UUID fleetId
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        return vehicleSafetyScoreRepository
                .findScoresByFleetId(fleetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VehicleSafetyScoreResponse toResponse(VehicleSafetyScore score) {
        Vehicle vehicle = score.getVehicle();

        return new VehicleSafetyScoreResponse(
                vehicle.getId(),
                "%s %s".formatted(vehicle.getMake(), vehicle.getModel()),
                vehicle.getLicensePlate(),
                score.getScore(),
                score.getHardBrakes(),
                score.getHardAccelerations(),
                score.getHarshTurns(),
                score.getSpeedingEvents(),
                score.getIdleMinutes(),
                score.isCheckEngine(),
                score.getMilesDriven()
        );
    }
}