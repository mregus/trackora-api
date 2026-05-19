package com.fleetwise.api.alert.service;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.fleetwise.api.alert.entity.AlertType.MAINTENANCE_DUE;
import static com.fleetwise.api.alert.entity.AlertType.MAINTENANCE_OVERDUE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceAlertService {

    private final MaintenanceRepository maintenanceRepository;
    private final AlertRepository alertRepository;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void generateMaintenanceDueAlerts() {
        int createdMileageAlerts = generateMileageBasedAlerts();
        int createdDateAlerts = generateDateBasedAlerts();

        log.info(
                "Maintenance alert generation completed. Mileage alerts: {}, Date alerts: {}",
                createdMileageAlerts,
                createdDateAlerts
        );
    }

    private int generateMileageBasedAlerts() {
        List<Maintenance> overdueMaintenance =
                maintenanceRepository.findScheduledMaintenanceOverdueByMileage();

        int created = 0;

        for (Maintenance maintenance : overdueMaintenance) {
            var vehicle = maintenance.getVehicle();

            boolean alertExists =
                    alertRepository.existsByVehicleIdAndTypeAndResolvedFalse(
                            vehicle.getId(),
                            MAINTENANCE_OVERDUE
                    );

            if (alertExists) {
                continue;
            }

            Alert alert = Alert.builder()
                    .fleet(vehicle.getFleet())
                    .vehicle(vehicle)
                    .type(MAINTENANCE_OVERDUE)
                    .severity(AlertSeverity.CRITICAL)
                    .message("""
                        Scheduled maintenance is overdue for %s %s.
                        Service type: %s
                        Target mileage: %s
                        Current mileage: %s
                        """.formatted(
                            vehicle.getMake(),
                            vehicle.getModel(),
                            maintenance.getServiceType(),
                            maintenance.getMileage(),
                            vehicle.getCurrentMileage()
                    ))
                    .resolved(false)
                    .build();

            alertRepository.save(alert);
            created++;
        }

        return created;
    }

    private int generateDateBasedAlerts() {
        LocalDate dueDate = LocalDate.now().plusDays(7);

        List<Maintenance> dueMaintenance =
                maintenanceRepository.findScheduledMaintenanceDueBy(dueDate);

        int created = 0;

        for (Maintenance maintenance : dueMaintenance) {
            var vehicle = maintenance.getVehicle();

            boolean alertExists =
                    alertRepository.existsByVehicleIdAndTypeAndResolvedFalse(
                            vehicle.getId(),
                            MAINTENANCE_DUE
                    );

            if (alertExists) {
                continue;
            }

            Alert alert = Alert.builder()
                    .fleet(vehicle.getFleet())
                    .vehicle(vehicle)
                    .type(MAINTENANCE_DUE)
                    .severity(AlertSeverity.WARNING)
                    .message("""
                        Scheduled maintenance is due soon for %s %s on %s.
                        Service type: %s
                        """.formatted(
                            vehicle.getMake(),
                            vehicle.getModel(),
                            maintenance.getServiceDate(),
                            maintenance.getServiceType()
                    ))
                    .resolved(false)
                    .build();

            alertRepository.save(alert);
            created++;
        }

        return created;
    }
}