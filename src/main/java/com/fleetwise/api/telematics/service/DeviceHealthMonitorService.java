package com.fleetwise.api.telematics.service;

import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.telematics.entity.VehicleCurrentState;
import com.fleetwise.api.telematics.repository.VehicleCurrentStateRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceHealthMonitorService {

    private final VehicleCurrentStateRepository vehicleCurrentStateRepository;
    private final TelematicsService telematicsService;
    private final AlertRepository alertRepository;

    @Transactional
    @Scheduled(fixedDelayString = "${telematics.device-health.check-delay-ms:300000}")
    public void checkDeviceHealth() {
        Instant now = Instant.now();
        Instant staleCutoff = now.minusSeconds(15 * 60);
        Instant offlineCutoff = now.minusSeconds(60 * 60);

        List<VehicleCurrentState> staleOrOffline =
                vehicleCurrentStateRepository.findStaleStatesWithVehicle(staleCutoff);

        for (VehicleCurrentState state : staleOrOffline) {
            Vehicle vehicle = state.getVehicle();

            if (vehicle == null || state.getLastSeenAt() == null) {
                continue;
            }

            AlertType type = state.getLastSeenAt().isBefore(offlineCutoff)
                    ? AlertType.DEVICE_OFFLINE
                    : AlertType.DEVICE_STALE;

            AlertSeverity severity = type == AlertType.DEVICE_OFFLINE
                    ? AlertSeverity.CRITICAL
                    : AlertSeverity.WARNING;

            if (!alertRepository.existsByVehicleIdAndTypeAndResolvedFalse(
                    vehicle.getId(),
                    type)) {

                telematicsService.createSystemAlert(
                        vehicle,
                        type,
                        severity,
                        "%s %s device is %s. Last seen: %s"
                                .formatted(
                                        vehicle.getMake(),
                                        vehicle.getModel(),
                                        type == AlertType.DEVICE_OFFLINE
                                                ? "offline"
                                                : "stale",
                                        state.getLastSeenAt()
                                )
                );
            }
        }
    }
}