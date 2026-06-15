package com.fleetwise.api.alert.scheduler;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertRepository alertRepository;
    private final VehicleRepository vehicleRepository;
    private final MaintenanceRepository maintenanceRepository;

    /** Runs daily at midnight */
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyAlertChecks() {
        log.info("Running daily alert checks...");

        // Example placeholder logic
        // Find vehicles that missed maintenance intervals or have anomalies
        vehicleRepository.findAll().forEach(v -> {
            // (Example) generate dummy alert for demonstration
            Alert alert = Alert.builder()
                    .fleet(v.getFleet())
                    .vehicle(v)
                    .severity(AlertSeverity.WARNING)
                    .type(AlertType.MAINTENANCE_DUE)
                    .message("Maintenance check due soon for vehicle: " + v.getMake() + " " + v.getModel())
                    .build();
            alertRepository.save(alert);
        });
    }
}
