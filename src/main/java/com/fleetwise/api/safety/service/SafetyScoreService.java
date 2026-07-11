package com.fleetwise.api.safety.service;

import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.safety.dto.FleetSafetyTrendPoint;
import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
import com.fleetwise.api.safety.entity.VehicleSafetyScore;
import com.fleetwise.api.safety.entity.VehicleSafetyScoreHistory;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreHistoryRepository;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SafetyScoreService {

    private final FleetAccessService fleetAccessService;
    private final VehicleSafetyScoreRepository vehicleSafetyScoreRepository;
    private final VehicleSafetyScoreHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public List<VehicleSafetyScoreResponse> getFleetSafetyScores(
            UUID userId,
            UUID fleetId
    ) {
        fleetAccessService.validateAccess(fleetId, userId);
        return getSystemFleetSafetyScores(fleetId);
    }

    @Transactional(readOnly = true)
    public List<VehicleSafetyScoreResponse> getSystemFleetSafetyScores(UUID fleetId) {
        return vehicleSafetyScoreRepository
                .findScoresByFleetId(fleetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FleetSafetyTrendPoint> getFleetSafetyTrend(
            UUID userId,
            UUID fleetId,
            int days
    ) {
        fleetAccessService.validateAccess(fleetId, userId);
        return getSystemFleetSafetyTrend(fleetId, days);
    }

    @Transactional(readOnly = true)
    public List<FleetSafetyTrendPoint> getSystemFleetSafetyTrend(
            UUID fleetId,
            int days
    ) {
        int safeDays = Math.max(1, Math.min(days, 365));

        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(safeDays - 1L);

        List<VehicleSafetyScoreHistory> history =
                historyRepository
                        .findByVehicleFleetIdAndScoreDateBetweenOrderByScoreDateAsc(
                                fleetId,
                                startDate,
                                endDate
                        );

        return history.stream()
                .collect(Collectors.groupingBy(
                        VehicleSafetyScoreHistory::getScoreDate,
                        TreeMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> toTrendPoint(entry.getKey(), entry.getValue()))
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

    private FleetSafetyTrendPoint toTrendPoint(
            LocalDate date,
            List<VehicleSafetyScoreHistory> snapshots
    ) {
        double averageScore = snapshots.stream()
                .mapToInt(VehicleSafetyScoreHistory::getScore)
                .average()
                .orElse(0);

        int hardBrakes = snapshots.stream()
                .mapToInt(VehicleSafetyScoreHistory::getHardBrakes)
                .sum();

        int hardAccelerations = snapshots.stream()
                .mapToInt(VehicleSafetyScoreHistory::getHardAccelerations)
                .sum();

        int harshTurns = snapshots.stream()
                .mapToInt(VehicleSafetyScoreHistory::getHarshTurns)
                .sum();

        int speedingEvents = snapshots.stream()
                .mapToInt(VehicleSafetyScoreHistory::getSpeedingEvents)
                .sum();

        int idleMinutes = snapshots.stream()
                .mapToInt(VehicleSafetyScoreHistory::getIdleMinutes)
                .sum();

        return new FleetSafetyTrendPoint(
                date,
                Math.round(averageScore * 10.0) / 10.0,
                hardBrakes,
                hardAccelerations,
                harshTurns,
                speedingEvents,
                idleMinutes
        );
    }
}