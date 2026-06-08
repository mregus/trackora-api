package com.fleetwise.api.telematics.service;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.notification.entity.NotificationType;
import com.fleetwise.api.notification.service.NotificationService;
import com.fleetwise.api.telematics.dto.CreateTelematicsEventRequest;
import com.fleetwise.api.telematics.dto.TelematicsEventResponse;
import com.fleetwise.api.telematics.entity.TelematicsEvent;
import com.fleetwise.api.telematics.repository.TelematicsEventRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelematicsService {

    private final TelematicsEventRepository telematicsEventRepository;
    private final VehicleRepository vehicleRepository;
    private final FleetAccessService fleetAccessService;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;

    @Transactional
    public TelematicsEventResponse createEvent(
            UUID userId,
            CreateTelematicsEventRequest request
    ) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateWriteAccess(vehicle.getFleet().getId(), userId);

        TelematicsEvent event = TelematicsEvent.builder()
                .vehicle(vehicle)
                .recordedAt(request.recordedAt() != null ? request.recordedAt() : Instant.now())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .speedMph(request.speedMph())
                .odometerMiles(request.odometerMiles())
                .fuelLevelPercent(request.fuelLevelPercent())
                .engineTempF(request.engineTempF())
                .batteryVoltage(request.batteryVoltage())
                .checkEngine(request.checkEngine())
                .harshBraking(request.harshBraking())
                .idleMinutes(request.idleMinutes() != null ? request.idleMinutes() : 0)
                .build();

        TelematicsEvent saved = telematicsEventRepository.save(event);
        generateTelematicsAlerts(vehicle, saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TelematicsEventResponse getLatestForVehicle(
            UUID userId,
            UUID vehicleId
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), userId);

        return telematicsEventRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No telematics data found"));
    }

    private void generateTelematicsAlerts(Vehicle vehicle, TelematicsEvent event) {
        if (isLessThan(event.getFuelLevelPercent(), BigDecimal.valueOf(15))) {
            createAlert(vehicle, AlertType.LOW_FUEL, AlertSeverity.WARNING,
                    "%s %s fuel level is below 15%%.".formatted(vehicle.getMake(), vehicle.getModel()));
        }

        if (isGreaterThan(event.getEngineTempF(), BigDecimal.valueOf(230))) {
            createAlert(vehicle, AlertType.ENGINE_OVERHEAT, AlertSeverity.CRITICAL,
                    "%s %s engine temperature is above safe range.".formatted(vehicle.getMake(), vehicle.getModel()));
        }

        if (isLessThan(event.getBatteryVoltage(), BigDecimal.valueOf(11.8))) {
            createAlert(vehicle, AlertType.LOW_BATTERY, AlertSeverity.WARNING,
                    "%s %s battery voltage is low.".formatted(vehicle.getMake(), vehicle.getModel()));
        }

        if (isGreaterThan(event.getSpeedMph(), BigDecimal.valueOf(85))) {
            createAlert(vehicle, AlertType.SPEEDING, AlertSeverity.WARNING,
                    "%s %s exceeded 85 mph.".formatted(vehicle.getMake(), vehicle.getModel()));
        }

        if (event.getIdleMinutes() != null && event.getIdleMinutes() > 30) {
            createAlert(vehicle, AlertType.EXCESSIVE_IDLE, AlertSeverity.WARNING,
                    "%s %s idled for more than 30 minutes.".formatted(vehicle.getMake(), vehicle.getModel()));
        }

        if (event.isCheckEngine()) {
            createAlert(vehicle, AlertType.CHECK_ENGINE, AlertSeverity.CRITICAL,
                    "%s %s reported a check engine fault.".formatted(vehicle.getMake(), vehicle.getModel()));
        }
    }

    private void createAlert(
            Vehicle vehicle,
            AlertType type,
            AlertSeverity severity,
            String message
    ) {
        Alert alert = Alert.builder()
                .fleet(vehicle.getFleet())
                .vehicle(vehicle)
                .type(type)
                .severity(severity)
                .message(message)
                .resolved(false)
                .build();

        alertRepository.save(alert);

        notificationService.create(
                vehicle.getFleet().getOwner().getId(),
                message,
                message,
                severity == AlertSeverity.CRITICAL
                        ? NotificationType.CRITICAL_ALERT
                        : NotificationType.SYSTEM
        );
    }

    private boolean isLessThan(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) < 0;
    }

    private boolean isGreaterThan(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) > 0;
    }

    private TelematicsEventResponse toResponse(TelematicsEvent event) {
        return new TelematicsEventResponse(
                event.getId(),
                event.getVehicle().getId(),
                event.getRecordedAt(),
                event.getLatitude(),
                event.getLongitude(),
                event.getSpeedMph(),
                event.getOdometerMiles(),
                event.getFuelLevelPercent(),
                event.getEngineTempF(),
                event.getBatteryVoltage(),
                event.isCheckEngine(),
                event.isHarshBraking(),
                event.getIdleMinutes()
        );
    }
}