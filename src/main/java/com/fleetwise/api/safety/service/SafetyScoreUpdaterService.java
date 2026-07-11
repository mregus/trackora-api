package com.fleetwise.api.safety.service;

import com.fleetwise.api.safety.entity.VehicleSafetyScore;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreRepository;
import com.fleetwise.api.telematics.entity.TelematicsEvent;
import com.fleetwise.api.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SafetyScoreUpdaterService {

    private final VehicleSafetyScoreRepository repository;
    private final SafetyScoreHistoryService safetyScoreHistoryService;

    public void updateFromEvent(
            Vehicle vehicle,
            TelematicsEvent event,
            String reasonText
    ) {
        VehicleSafetyScore score = repository.findByVehicleId(vehicle.getId())
                .orElseGet(() -> VehicleSafetyScore.builder()
                        .vehicle(vehicle)
                        .score(100)
                        .hardBrakes(0)
                        .hardAccelerations(0)
                        .harshTurns(0)
                        .speedingEvents(0)
                        .idleMinutes(0)
                        .checkEngine(false)
                        .milesDriven(BigDecimal.ZERO)
                        .build());

        if ("HARDBRAKE".equalsIgnoreCase(reasonText)) {
            score.setHardBrakes(score.getHardBrakes() + 1);
        }

        if ("HARDACCEL".equalsIgnoreCase(reasonText)) {
            score.setHardAccelerations(score.getHardAccelerations() + 1);
        }

        if ("HARDTURN".equalsIgnoreCase(reasonText)) {
            score.setHarshTurns(score.getHarshTurns() + 1);
        }

        if (event.getSpeedMph() != null &&
                event.getSpeedMph().compareTo(BigDecimal.valueOf(85)) > 0) {
            score.setSpeedingEvents(score.getSpeedingEvents() + 1);
        }

        if (event.getIdleMinutes() != null) {
            score.setIdleMinutes(score.getIdleMinutes() + event.getIdleMinutes());
        }

        if (event.isCheckEngine()) {
            score.setCheckEngine(true);
        }

        score.setMilesDriven(event.getOdometerMiles());

        score.setScore(calculateScore(score));

        VehicleSafetyScore saved = repository.save(score);

        safetyScoreHistoryService.saveDailySnapshot(saved);
    }

    private int calculateScore(VehicleSafetyScore score) {
        int value = 100;

        value -= score.getHardBrakes() * 5;
        value -= score.getHardAccelerations() * 4;
        value -= score.getHarshTurns() * 4;
        value -= score.getSpeedingEvents() * 3;
        value -= score.getIdleMinutes() / 10;

        if (score.isCheckEngine()) {
            value -= 10;
        }

        return Math.max(0, Math.min(100, value));
    }
}