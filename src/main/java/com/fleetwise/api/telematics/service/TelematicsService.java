package com.fleetwise.api.telematics.service;

import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.notification.entity.NotificationType;
import com.fleetwise.api.notification.service.NotificationService;
import com.fleetwise.api.telematics.dto.*;
import com.fleetwise.api.telematics.entity.TelematicsDevice;
import com.fleetwise.api.telematics.entity.TelematicsEvent;
import com.fleetwise.api.telematics.entity.TelematicsProvider;
import com.fleetwise.api.telematics.geometris.*;
import com.fleetwise.api.telematics.repository.TelematicsDeviceRepository;
import com.fleetwise.api.telematics.repository.TelematicsEventRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelematicsService {

    private final TelematicsDeviceRepository telematicsDeviceRepository;
    private final TelematicsEventRepository telematicsEventRepository;
    private final VehicleRepository vehicleRepository;
    private final FleetAccessService fleetAccessService;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final GeometrisPacketParser geometrisPacketParser;
    private final GeometrisRawPacketRepository geometrisRawPacketRepository;

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

    @Transactional
    public TelematicsDeviceResponse registerDevice(
            UUID userId,
            RegisterTelematicsDeviceRequest request
    ) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateWriteAccess(vehicle.getFleet().getId(), userId);

        TelematicsDevice device = TelematicsDevice.builder()
                .vehicle(vehicle)
                .provider(request.provider())
                .externalDeviceId(request.externalDeviceId())
                .serialNumber(request.serialNumber())
                .imei(request.imei())
                .vin(request.vin())
                .active(true)
                .build();

        return toDeviceResponse(telematicsDeviceRepository.save(device));
    }

    @Transactional(readOnly = true)
    public List<TelematicsDeviceResponse> getDevicesForVehicle(
            UUID userId,
            UUID vehicleId
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), userId);

        return telematicsDeviceRepository.findByVehicleIdAndActiveTrue(vehicleId)
                .stream()
                .map(this::toDeviceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GeometrisRawPacketResponse> getLatestGeometrisRawPackets() {
        return geometrisRawPacketRepository.findTop50ByOrderByReceivedAtDesc()
                .stream()
                .map(this::toRawPacketResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GeometrisRawPacketResponse> getFailedGeometrisRawPackets() {
        return geometrisRawPacketRepository.findTop50ByParsedSuccessfullyFalseOrderByReceivedAtDesc()
                .stream()
                .map(this::toRawPacketResponse)
                .toList();
    }

    private GeometrisRawPacketResponse toRawPacketResponse(GeometrisRawPacket packet) {
        return new GeometrisRawPacketResponse(
                packet.getId(),
                packet.getSerialNumber(),
                packet.getReasonText(),
                packet.isParsedSuccessfully(),
                packet.getErrorMessage(),
                packet.getReceivedAt()
        );
    }

    private TelematicsDeviceResponse toDeviceResponse(TelematicsDevice device) {
        return new TelematicsDeviceResponse(
                device.getId(),
                device.getVehicle().getId(),
                device.getProvider(),
                device.getExternalDeviceId(),
                device.getSerialNumber(),
                device.getImei(),
                device.getVin(),
                device.isActive(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                device.getLastSeenAt()
        );
    }

    @Transactional
    public TelematicsEventResponse ingestGeometrisPacket(String rawPacket) {
        try {
            GeometrisPacket packet = geometrisPacketParser.parse(rawPacket);

            geometrisRawPacketRepository.save(
                    GeometrisRawPacket.builder()
                            .serialNumber(packet.serialNumber())
                            .reasonText(packet.reasonText())
                            .rawPacket(rawPacket)
                            .parsedSuccessfully(true)
                            .build()
            );

            TelematicsDevice device = telematicsDeviceRepository
                    .findByProviderAndSerialNumberAndActiveTrue(
                            TelematicsProvider.GEOMETRIS,
                            packet.serialNumber()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active Geometris device mapping found"
                    ));

            device.setLastSeenAt(packet.recordedAt() != null ? packet.recordedAt() : Instant.now());

            Vehicle vehicle = device.getVehicle();

            TelematicsEvent event = TelematicsEvent.builder()
                    .vehicle(vehicle)
                    .recordedAt(packet.recordedAt())
                    .latitude(packet.latitude())
                    .longitude(packet.longitude())
                    .speedMph(packet.speedMph())
                    .odometerMiles(packet.ecuOdometerMiles() != null
                            ? packet.ecuOdometerMiles()
                            : packet.gpsOdometerMiles())
                    .fuelLevelPercent(packet.fuelLevelPercent())
                    .engineTempF(celsiusToFahrenheit(packet.coolantTempC()))
                    .batteryVoltage(packet.batteryVoltage())
                    .checkEngine(hasActiveDtc(packet.activeDtc()))
                    .headingDegrees(packet.headingDegrees())
                    .idleMinutes(secondsToMinutes(packet.totalIdleDurationSeconds()))
                    .harshBraking("HARDBRAKE".equalsIgnoreCase(packet.reasonText()))
                    .build();

            TelematicsEvent saved = telematicsEventRepository.save(event);

            generateTelematicsAlerts(vehicle, saved);

            return toResponse(saved);

        } catch (Exception ex) {
            geometrisRawPacketRepository.save(
                    GeometrisRawPacket.builder()
                            .rawPacket(rawPacket)
                            .parsedSuccessfully(false)
                            .errorMessage(ex.getMessage())
                            .build()
            );

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<FleetTelematicsLocationResponse> getLatestFleetLocations(
            UUID userId,
            UUID fleetId
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        return telematicsEventRepository.findLatestByFleetId(fleetId)
                .stream()
                .filter(event -> event.getLatitude() != null && event.getLongitude() != null)
                .map(event -> {
                    Vehicle vehicle = event.getVehicle();

                    return new FleetTelematicsLocationResponse(
                            vehicle.getId(),
                            "%s %s".formatted(vehicle.getMake(), vehicle.getModel()),
                            vehicle.getLicensePlate(),
                            event.getLatitude(),
                            event.getLongitude(),
                            event.getSpeedMph(),
                            event.getFuelLevelPercent(),
                            event.isCheckEngine(),
                            event.getHeadingDegrees(),
                            event.getRecordedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TelematicsHistoryPointResponse> getVehicleHistory(
            UUID userId,
            UUID vehicleId
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), userId);

        Instant start = Instant.now().minus(24, ChronoUnit.HOURS);

        return telematicsEventRepository.findHistoryByVehicleId(vehicleId, start)
                .stream()
                .map(event -> new TelematicsHistoryPointResponse(
                        event.getLatitude(),
                        event.getLongitude(),
                        event.getSpeedMph(),
                        event.getRecordedAt()
                ))
                .toList();
    }

    private BigDecimal celsiusToFahrenheit(Integer celsius) {
        if (celsius == null) {
            return null;
        }

        return BigDecimal.valueOf((celsius * 9.0 / 5.0) + 32);
    }

    private Integer secondsToMinutes(Integer seconds) {
        if (seconds == null) {
            return 0;
        }

        return seconds / 60;
    }

    private boolean hasActiveDtc(String dtc) {
        return dtc != null
                && !dtc.isBlank()
                && !"0:0".equals(dtc)
                && !"0".equals(dtc);
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