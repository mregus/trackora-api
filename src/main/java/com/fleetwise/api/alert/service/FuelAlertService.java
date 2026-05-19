package com.fleetwise.api.alert.service;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.fuel.entity.FuelLog;
import com.fleetwise.api.fuel.repository.FuelLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelAlertService {

    private static final BigDecimal THRESHOLD_MULTIPLIER = new BigDecimal("1.20");

    private final FuelLogRepository fuelLogRepository;
    private final AlertRepository alertRepository;

    @Scheduled(cron = "0 15 6 * * *")
    @Transactional
    public void generateFuelAnomalyAlerts() {
        List<UUID> fleetIds = fuelLogRepository.findFleetIdsWithFuelLogs();

        int created = 0;

        for (UUID fleetId : fleetIds) {
            created += generateForFleet(fleetId);
        }

        log.info("Fuel anomaly alert generation completed. Created {} alerts.", created);
    }

    private int generateForFleet(UUID fleetId) {
        List<FuelLog> logs = fuelLogRepository.findByVehicleFleetId(fleetId);

        if (logs.size() < 2) {
            return 0;
        }

        BigDecimal total = logs.stream()
                .map(FuelLog::getPricePerGallon)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = total.divide(
                BigDecimal.valueOf(logs.size()),
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal threshold = average.multiply(THRESHOLD_MULTIPLIER);

        int created = 0;

        for (FuelLog log : logs) {
            if (log.getPricePerGallon().compareTo(threshold) <= 0) {
                continue;
            }

            var vehicle = log.getVehicle();

            boolean alertExists =
                    alertRepository.existsByVehicleIdAndTypeAndResolvedFalse(
                            vehicle.getId(),
                            AlertType.FUEL_ANOMALY
                    );

            if (alertExists) {
                continue;
            }

            Alert alert = Alert.builder()
                    .fleet(vehicle.getFleet())
                    .vehicle(vehicle)
                    .type(AlertType.FUEL_ANOMALY)
                    .severity(AlertSeverity.WARNING)
                    .message("""
                            Fuel price anomaly detected for %s %s.
                            Price per gallon: $%s
                            Fleet average: $%s
                            """.formatted(
                            vehicle.getMake(),
                            vehicle.getModel(),
                            log.getPricePerGallon(),
                            average
                    ))
                    .resolved(false)
                    .build();

            alertRepository.save(alert);
            created++;
        }

        return created;
    }
}