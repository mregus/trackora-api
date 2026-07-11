package com.fleetwise.api.safety.service;

import com.fleetwise.api.safety.entity.VehicleSafetyScore;
import com.fleetwise.api.safety.entity.VehicleSafetyScoreHistory;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SafetyScoreHistoryService {

    private final VehicleSafetyScoreHistoryRepository historyRepository;

    @Transactional
    public void saveDailySnapshot(VehicleSafetyScore currentScore) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        VehicleSafetyScoreHistory history = historyRepository
                .findByVehicleIdAndScoreDate(
                        currentScore.getVehicle().getId(),
                        today
                )
                .orElseGet(() -> VehicleSafetyScoreHistory.builder()
                        .vehicle(currentScore.getVehicle())
                        .scoreDate(today)
                        .build());

        history.setScore(currentScore.getScore());
        history.setHardBrakes(currentScore.getHardBrakes());
        history.setHardAccelerations(currentScore.getHardAccelerations());
        history.setHarshTurns(currentScore.getHarshTurns());
        history.setSpeedingEvents(currentScore.getSpeedingEvents());
        history.setIdleMinutes(currentScore.getIdleMinutes());
        history.setCheckEngine(currentScore.isCheckEngine());
        history.setMilesDriven(currentScore.getMilesDriven());
        history.setRecordedAt(Instant.now());

        historyRepository.save(history);
    }
}