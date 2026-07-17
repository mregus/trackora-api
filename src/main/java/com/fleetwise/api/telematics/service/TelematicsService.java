package com.fleetwise.api.telematics.service;

import com.fleetwise.api.alert.dto.LiveAlertEvent;
import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.entity.AlertType;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.dashboard.DashboardRealtimePublisher;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.notification.entity.NotificationType;
import com.fleetwise.api.notification.service.NotificationService;
import com.fleetwise.api.safety.service.SafetyScoreUpdaterService;
import com.fleetwise.api.safety.websocket.SafetyScoreRealtimePublisher;
import com.fleetwise.api.telematics.dto.*;
import com.fleetwise.api.telematics.entity.*;
import com.fleetwise.api.telematics.geometris.*;
import com.fleetwise.api.telematics.observability.TelematicsMetrics;
import com.fleetwise.api.telematics.observability.TelemetryFailureReason;
import com.fleetwise.api.telematics.observability.TelemetrySource;
import com.fleetwise.api.telematics.repository.RawTelematicsPacketRepository;
import com.fleetwise.api.telematics.repository.TelematicsDeviceRepository;
import com.fleetwise.api.telematics.repository.TelematicsEventRepository;
import com.fleetwise.api.telematics.repository.VehicleCurrentStateRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    Logger logger = LoggerFactory.getLogger(TelematicsService.class);

    private static final BigDecimal IDLE_SPEED_THRESHOLD_MPH =
            BigDecimal.valueOf(2);

    private final GeometrisGpsTrailDecoder gpsTrailDecoder;
    private final TelematicsDeviceRepository telematicsDeviceRepository;
    private final TelematicsEventRepository telematicsEventRepository;
    private final VehicleRepository vehicleRepository;
    private final FleetAccessService fleetAccessService;
    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final GeometrisPacketParser geometrisPacketParser;
    private final GeometrisRawPacketRepository geometrisRawPacketRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RawTelematicsPacketRepository rawTelematicsPacketRepository;
    private final TelematicsMetrics telematicsMetrics;
    private final VehicleCurrentStateRepository vehicleCurrentStateRepository;
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final SafetyScoreUpdaterService safetyScoreUpdaterService;
    private final SafetyScoreRealtimePublisher safetyScoreRealtimePublisher;

    public void createSystemAlert(
            Vehicle vehicle,
            AlertType type,
            AlertSeverity severity,
            String message
    ) {
        createAlert(vehicle, type, severity, message);
    }

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

        TelematicsEvent saved = telematicsMetrics.record(
                telematicsMetrics.getEventSaveTimer(),
                () -> telematicsEventRepository.save(event)
        );

        VehicleCurrentState state =
                getOrCreateCurrentState(vehicle);

        updateManualEventState(
                state,
                saved
        );

        vehicleCurrentStateRepository.save(state);

        generateTelematicsAlerts(vehicle, saved, null);

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

    @Transactional
    public void ingestGeometrisPacket(String rawPacket) {
        processGeometrisPacket(rawPacket, TelemetrySource.HTTP);
    }

    @Transactional
    public void ingestGeometrisPacketEntity(String rawPacket, TelemetrySource source) {
        processGeometrisPacket(rawPacket, source);
    }

    @Transactional(readOnly = true)
    public List<FleetTelematicsLocationResponse> getLatestFleetLocations(
            UUID userId,
            UUID fleetId
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        return vehicleCurrentStateRepository.findByVehicleFleetId(fleetId)
                .stream()
                .filter(state -> state.getLatitude() != null && state.getLongitude() != null)
                .map(state -> {
                    Vehicle vehicle = state.getVehicle();

                    return new FleetTelematicsLocationResponse(
                            vehicle.getId(),
                            getVehicleStatus(state.getLastSeenAt()),
                            "%s %s".formatted(vehicle.getMake(), vehicle.getModel()),
                            vehicle.getLicensePlate(),
                            state.getLatitude(),
                            state.getLongitude(),
                            state.getSpeedMph(),
                            state.getFuelLevelPercent(),
                            state.isCheckEngine(),
                            state.getHeadingDegrees(),
                            state.getLastSeenAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TelematicsHistoryPointResponse> getVehicleHistory(
            UUID userId,
            UUID vehicleId,
            Instant start,
            Instant end
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        fleetAccessService.validateAccess(vehicle.getFleet().getId(), userId);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        return telematicsEventRepository.findHistoryByVehicleId(vehicleId, start, end)
                .stream()
                .map(event -> new TelematicsHistoryPointResponse(
                        event.getLatitude(),
                        event.getLongitude(),
                        event.getSpeedMph(),
                        event.getRecordedAt(),
                        event.getVehicle().getId(),
                        event.getVehicle().getMake(),
                        event.getVehicle().getModel(),
                        event.getVehicle().getLicensePlate()
                ))
                .toList();
    }

    private void updateEventAndIdleState(
            VehicleCurrentState state,
            TelematicsEvent event,
            GeometrisPacket packet
    ) {
        Instant recordedAt = event.getRecordedAt();

        if (
                recordedAt != null
                        && state.getLastSeenAt() != null
                        && recordedAt.isBefore(state.getLastSeenAt())
        ) {
            /*
             * Keep the event for history, but do not let an older packet
             * overwrite current state or current idle-session tracking.
             */
            return;
        }

        if (event.getLatitude() != null) {
            state.setLatitude(event.getLatitude());
        }

        if (event.getLongitude() != null) {
            state.setLongitude(event.getLongitude());
        }

        if (event.getSpeedMph() != null) {
            state.setSpeedMph(event.getSpeedMph());
        }

        if (event.getHeadingDegrees() != null) {
            state.setHeadingDegrees(event.getHeadingDegrees());
        }

        if (event.getFuelLevelPercent() != null) {
            state.setFuelLevelPercent(
                    event.getFuelLevelPercent()
            );
        }

        /*
         * Only change check-engine state when the packet actually
         * includes the DTC field.
         */
        if (packet != null && packet.activeDtc() != null) {
            state.setCheckEngine(event.isCheckEngine());
        }

        updateIdleState(state, event, packet);

        state.setLastSeenAt(
                event.getRecordedAt() != null
                        ? event.getRecordedAt()
                        : Instant.now()
        );
    }

    private void resolveExcessiveIdleAlertIfCleared(
            Vehicle vehicle,
            TelematicsEvent event
    ) {
        if (event.getIdleMinutes() != null
                && event.getIdleMinutes() >= 30) {
            return;
        }

        alertRepository
                .findByVehicleIdAndTypeInAndResolvedFalse(
                        vehicle.getId(),
                        List.of(AlertType.EXCESSIVE_IDLE)
                )
                .forEach(alert -> {
                    alert.setResolved(true);
                    alert.setResolvedAt(Instant.now());
                    alertRepository.save(alert);
                });
    }

    private void processGeometrisPacket(String rawPacket, TelemetrySource source) {
        telematicsMetrics.packetReceived(source);
        Timer.Sample sample = telematicsMetrics.startTimer();
        long startMs = System.currentTimeMillis();
        RawTelematicsPacket raw = saveRawPacket(rawPacket);

        try {
            GeometrisPacket packet = telematicsMetrics.record(
                    telematicsMetrics.getParseTimer(),
                    () -> geometrisPacketParser.parse(rawPacket)
            );

            telematicsMetrics.packetType(
                    source,
                    packet.formatCrc(),
                    packet.reasonText()
            );

            raw.setDeviceSerial(packet.serialNumber());
            raw.setPacketType(packet.formatCrc());

            GeometrisRawPacket geometrisRawPacket = GeometrisRawPacket.builder()
                    .serialNumber(packet.serialNumber())
                    .reasonText(packet.reasonText())
                    .rawPacket(rawPacket)
                    .parsedSuccessfully(true)
                    .build();

            geometrisRawPacketRepository.save(geometrisRawPacket);

            TelematicsDevice device = telematicsDeviceRepository
                    .findByProviderAndSerialNumberAndActiveTrue(
                            TelematicsProvider.GEOMETRIS,
                            packet.serialNumber()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active Geometris device mapping found"
                    ));

            device.setLastSeenAt(
                    packet.recordedAt() != null ? packet.recordedAt() : Instant.now()
            );

            Vehicle vehicle = device.getVehicle();

            TelematicsEvent event =
                    toTelematicsEvent(vehicle, packet);

            VehicleCurrentState state =
                    getOrCreateCurrentState(vehicle);

            updateEventAndIdleState(
                    state,
                    event,
                    packet
            );

            TelematicsEvent saved = telematicsMetrics.record(
                    telematicsMetrics.getEventSaveTimer(),
                    () -> telematicsEventRepository.save(event)
            );

            vehicleCurrentStateRepository.save(state);

            resolveExcessiveIdleAlertIfCleared(
                    vehicle,
                    saved
            );

            // Device reported again, so resolve stale/offline alerts
            resolveOpenDeviceAlerts(vehicle);

            if (packet.locationTrail() != null && !packet.locationTrail().isBlank()) {
                saveTrailPoints(vehicle, packet, saved);
            }

            messagingTemplate.convertAndSend(
                    "/topic/fleets/" + vehicle.getFleet().getId(),
                    toLiveEvent(saved)
            );

            generateTelematicsAlerts(
                    vehicle,
                    saved,
                    packet
            );
            generateDriverBehaviorAlerts(vehicle, packet);

            safetyScoreUpdaterService.updateFromEvent(vehicle, saved, packet.reasonText());

            // Publish live safety score dashboard update
            safetyScoreRealtimePublisher.publish(vehicle.getFleet().getId());

            // Publish live dashboard update
            dashboardRealtimePublisher.publish(vehicle.getFleet().getId());

            raw.setProcessed(true);
            rawTelematicsPacketRepository.save(raw);

            telematicsMetrics.packetProcessed(source);

            logger.info(
                    "Telemetry packet processed source={} serial={} format={} reason={} vehicleId={} fleetId={} recordedAt={} processingMs={}",
                    source,
                    packet.serialNumber(),
                    packet.formatCrc(),
                    packet.reasonText(),
                    vehicle.getId(),
                    vehicle.getFleet().getId(),
                    packet.recordedAt(),
                    System.currentTimeMillis() - startMs
            );

        } catch (Exception ex) {
            telematicsMetrics.packetFailed(source, classifyFailure(ex));

            geometrisRawPacketRepository.save(
                    GeometrisRawPacket.builder()
                            .rawPacket(rawPacket)
                            .parsedSuccessfully(false)
                            .errorMessage(ex.getMessage())
                            .build()
            );

            raw.setProcessed(false);
            raw.setErrorMessage(ex.getMessage());
            rawTelematicsPacketRepository.save(raw);

            logger.warn(
                    "Telemetry packet failed source={} failureReason={} processingMs={} error={}",
                    source,
                    classifyFailure(ex),
                    System.currentTimeMillis() - startMs,
                    ex.getMessage()
            );

            throw ex;
        } finally {
            telematicsMetrics.stopTimer(sample, source);
        }
    }

    private VehicleCurrentState getOrCreateCurrentState(
            Vehicle vehicle
    ) {
        return vehicleCurrentStateRepository
                .findById(vehicle.getId())
                .orElseGet(() -> {
                    VehicleCurrentState state =
                            new VehicleCurrentState();

                    state.setVehicle(vehicle);
                    state.setCurrentIdleMinutes(0);

                    return state;
                });
    }

    private BigDecimal celsiusToFahrenheit(Integer celsius) {
        if (celsius == null) {
            return null;
        }

        return BigDecimal.valueOf((celsius * 9.0 / 5.0) + 32);
    }

    private boolean hasActiveDtc(String dtc) {
        return dtc != null
                && !dtc.isBlank()
                && !"0:0".equals(dtc)
                && !"0".equals(dtc);
    }

    private void generateTelematicsAlerts(
            Vehicle vehicle,
            TelematicsEvent event,
            GeometrisPacket packet
    ) {
        if (
                packet != null && packet.fuelLevelPercent() != null
                        && isLessThan(
                        event.getFuelLevelPercent(),
                        BigDecimal.valueOf(15)
                )
        ) {
            createAlert(
                    vehicle,
                    AlertType.LOW_FUEL,
                    AlertSeverity.WARNING,
                    "%s %s fuel level is below 15%%."
                            .formatted(
                                    vehicle.getMake(),
                                    vehicle.getModel()
                            )
            );
        }

        if (
                packet != null && packet.coolantTempC() != null
                        && isGreaterThan(
                        event.getEngineTempF(),
                        BigDecimal.valueOf(230)
                )
        ) {
            createAlert(
                    vehicle,
                    AlertType.ENGINE_OVERHEAT,
                    AlertSeverity.CRITICAL,
                    "%s %s engine temperature is above safe range."
                            .formatted(
                                    vehicle.getMake(),
                                    vehicle.getModel()
                            )
            );
        }

        if (
                packet != null && packet.batteryVoltage() != null
                        && isLessThan(
                        event.getBatteryVoltage(),
                        BigDecimal.valueOf(11.8)
                )
        ) {
            createAlert(
                    vehicle,
                    AlertType.LOW_BATTERY,
                    AlertSeverity.WARNING,
                    "%s %s battery voltage is low."
                            .formatted(
                                    vehicle.getMake(),
                                    vehicle.getModel()
                            )
            );
        }

        if (
                event.getSpeedMph() != null
                        && isGreaterThan(
                        event.getSpeedMph(),
                        BigDecimal.valueOf(85)
                )
        ) {
            createAlert(
                    vehicle,
                    AlertType.SPEEDING,
                    AlertSeverity.WARNING,
                    "%s %s exceeded 85 mph."
                            .formatted(
                                    vehicle.getMake(),
                                    vehicle.getModel()
                            )
            );
        }

        if (
                event.getIdleMinutes() != null
                        && event.getIdleMinutes() >= 30
        ) {
            createAlert(
                    vehicle,
                    AlertType.EXCESSIVE_IDLE,
                    AlertSeverity.WARNING,
                    "%s %s has been idling for %d minutes."
                            .formatted(
                                    vehicle.getMake(),
                                    vehicle.getModel(),
                                    event.getIdleMinutes()
                            )
            );
        }

        if (
                packet != null && packet.activeDtc() != null
                        && event.isCheckEngine()
        ) {
            createAlert(
                    vehicle,
                    AlertType.CHECK_ENGINE,
                    AlertSeverity.CRITICAL,
                    "%s %s reported a check engine fault."
                            .formatted(
                                    vehicle.getMake(),
                                    vehicle.getModel()
                            )
            );
        }
    }

    private void createAlert(
            Vehicle vehicle,
            AlertType type,
            AlertSeverity severity,
            String message
    ) {
        boolean duplicateOpenAlert =
                alertRepository.existsByVehicleIdAndTypeAndResolvedFalse(
                        vehicle.getId(),
                        type
                );

        if (duplicateOpenAlert) {
            logger.debug(
                    "Skipping duplicate open alert vehicleId={}, type={}",
                    vehicle.getId(),
                    type
            );
            return;
        }

        Alert alert = Alert.builder()
                .fleet(vehicle.getFleet())
                .vehicle(vehicle)
                .type(type)
                .severity(severity)
                .message(message)
                .resolved(false)
                .build();

        Alert saved = alertRepository.save(alert);

        notificationService.create(
                vehicle.getFleet().getOwner().getId(),
                message,
                message,
                severity == AlertSeverity.CRITICAL
                        ? NotificationType.CRITICAL_ALERT
                        : NotificationType.SYSTEM
        );

        messagingTemplate.convertAndSend(
                "/topic/fleets/" + vehicle.getFleet().getId() + "/alerts",
                new LiveAlertEvent(
                        saved.getId(),
                        vehicle.getFleet().getId(),
                        vehicle.getId(),
                        "%s %s".formatted(
                                vehicle.getMake(),
                                vehicle.getModel()
                        ),
                        vehicle.getLicensePlate(),
                        saved.getType().name(),
                        saved.getSeverity().name(),
                        saved.getMessage(),
                        saved.getCreatedAt()
                )
        );

        logger.info(
                "Created and published alert alertId={}, vehicleId={}, type={}",
                saved.getId(),
                vehicle.getId(),
                type
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

    private LiveVehicleLocationEvent toLiveEvent(TelematicsEvent saved) {
        Vehicle vehicle = saved.getVehicle();

        return new LiveVehicleLocationEvent(
                vehicle.getId(),
                vehicle.getFleet().getId(),
                "%s %s".formatted(vehicle.getMake(), vehicle.getModel()),
                vehicle.getLicensePlate(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getSpeedMph(),
                saved.getHeadingDegrees(),
                saved.getFuelLevelPercent(),
                saved.isCheckEngine(),
                saved.getRecordedAt()
        );
    }

    private void saveTrailPoints(
            Vehicle vehicle,
            GeometrisPacket packet,
            TelematicsEvent latestEvent
    ) {

        List<GeometrisGpsTrailPoint> trailPoints =
                gpsTrailDecoder.decode(
                        packet.latitude(),
                        packet.longitude(),
                        packet.recordedAt(),
                        packet.locationTrail()
                );

        for (GeometrisGpsTrailPoint point : trailPoints) {

            TelematicsEvent trailEvent =
                    TelematicsEvent.builder()
                            .vehicle(vehicle)
                            .recordedAt(point.recordedAt())
                            .latitude(point.latitude())
                            .longitude(point.longitude())
                            .speedMph(point.speedMph())
                            .fuelLevelPercent(latestEvent.getFuelLevelPercent())
                            .headingDegrees(latestEvent.getHeadingDegrees())
//                            .ignitionOn(latestEvent.isIgnitionOn())
                            .build();

            telematicsMetrics.record(
                    telematicsMetrics.getEventSaveTimer(),
                    () -> telematicsEventRepository.save(trailEvent)
            );
        }
    }

    private RawTelematicsPacket saveRawPacket(String rawPacket) {
        String[] fields = rawPacket == null ? new String[0] : rawPacket.split(",", -1);

        String packetType = fields.length > 0 ? fields[0] : "UNKNOWN";
        String serial = fields.length > 1 ? fields[1] : null;

        return rawTelematicsPacketRepository.save(
                RawTelematicsPacket.builder()
                        .deviceSerial(serial)
                        .packetType(packetType)
                        .rawPayload(rawPacket)
                        .processed(false)
                        .build()
        );
    }

    private void generateDriverBehaviorAlerts(Vehicle vehicle, GeometrisPacket packet) {
        String reason = packet.reasonText();

        if (reason == null || reason.isBlank()) {
            return;
        }

        logger.info("Driver behavior reason received: {}", reason);

        switch (reason.toUpperCase()) {
            case "HARDBRAKE" -> createAlert(
                    vehicle,
                    AlertType.HARSH_BRAKING,
                    AlertSeverity.WARNING,
                    "%s %s harsh braking detected."
                            .formatted(vehicle.getMake(), vehicle.getModel())
            );

            case "HARDACCEL" -> createAlert(
                    vehicle,
                    AlertType.HARSH_ACCELERATION,
                    AlertSeverity.WARNING,
                    "%s %s harsh acceleration detected."
                            .formatted(vehicle.getMake(), vehicle.getModel())
            );

            case "HARDTURN" -> createAlert(
                    vehicle,
                    AlertType.HARSH_TURN,
                    AlertSeverity.WARNING,
                    "%s %s harsh turn detected."
                            .formatted(vehicle.getMake(), vehicle.getModel())
            );

            case "HARDSTOP" -> createAlert(
                    vehicle,
                    AlertType.HARD_STOP,
                    AlertSeverity.CRITICAL,
                    "%s %s hard stop / possible impact detected."
                            .formatted(vehicle.getMake(), vehicle.getModel())
            );

            case "SPEEDING" -> createAlert(
                    vehicle,
                    AlertType.SPEEDING,
                    AlertSeverity.WARNING,
                    "%s %s speeding event detected."
                            .formatted(vehicle.getMake(), vehicle.getModel())
            );
        }
    }

    private String getVehicleStatus(Instant lastSeenAt) {
        if (lastSeenAt == null) {
            return "OFFLINE";
        }

        long minutes = ChronoUnit.MINUTES.between(lastSeenAt, Instant.now());

        if (minutes <= 5) {
            return "ONLINE";
        }

        if (minutes <= 15) {
            return "STALE";
        }

        return "OFFLINE";
    }

    private void resolveOpenDeviceAlerts(Vehicle vehicle) {
        alertRepository
                .findByVehicleIdAndTypeInAndResolvedFalse(
                        vehicle.getId(),
                        List.of(AlertType.DEVICE_STALE, AlertType.DEVICE_OFFLINE)
                )
                .forEach(alert -> {
                    alert.setResolved(true);
                    alert.setResolvedAt(Instant.now());
                    alertRepository.save(alert);
                });
    }

    private TelemetryFailureReason classifyFailure(Exception ex) {
        if (ex instanceof IllegalArgumentException ||
                ex instanceof NumberFormatException) {
            return TelemetryFailureReason.PARSE_ERROR;
        }

        if (ex instanceof ResourceNotFoundException) {
            return TelemetryFailureReason.DEVICE_NOT_FOUND;
        }

        if (ex instanceof DataAccessException) {
            return TelemetryFailureReason.DATABASE_ERROR;
        }

        return TelemetryFailureReason.UNKNOWN;
    }

    private void updateManualEventState(
            VehicleCurrentState state,
            TelematicsEvent event
    ) {
        if (event.getLatitude() != null) {
            state.setLatitude(event.getLatitude());
        }

        if (event.getLongitude() != null) {
            state.setLongitude(event.getLongitude());
        }

        if (event.getSpeedMph() != null) {
            state.setSpeedMph(event.getSpeedMph());
        }

        if (event.getHeadingDegrees() != null) {
            state.setHeadingDegrees(event.getHeadingDegrees());
        }

        if (event.getFuelLevelPercent() != null) {
            state.setFuelLevelPercent(
                    event.getFuelLevelPercent()
            );
        }

        state.setCheckEngine(event.isCheckEngine());

        state.setLastSeenAt(
                event.getRecordedAt() != null
                        ? event.getRecordedAt()
                        : Instant.now()
        );
    }

    private void updateIdleState(
            VehicleCurrentState state,
            TelematicsEvent event,
            GeometrisPacket packet
    ) {
        Instant recordedAt = event.getRecordedAt() != null
                ? event.getRecordedAt()
                : Instant.now();

        /*
         * Update ignition only when this packet contains ignition data.
         * A 5873 packet does not, so it must not turn ignition off.
         */
        if (packet != null && packet.ignitionOn() != null) {
            state.setIgnitionOn(packet.ignitionOn());
        }

        Boolean ignitionOn = state.getIgnitionOn();
        BigDecimal speedMph = event.getSpeedMph();

        boolean stationary =
                speedMph != null
                        && speedMph.compareTo(
                        IDLE_SPEED_THRESHOLD_MPH
                ) <= 0;

        boolean currentlyIdling =
                Boolean.TRUE.equals(ignitionOn)
                        && stationary;

        if (!currentlyIdling) {
            state.setIdleStartedAt(null);
            state.setCurrentIdleMinutes(0);
            event.setIdleMinutes(0);
            return;
        }

        if (state.getIdleStartedAt() == null) {
            state.setIdleStartedAt(recordedAt);
        }

        long idleMinutes = Math.max(
                0,
                ChronoUnit.MINUTES.between(
                        state.getIdleStartedAt(),
                        recordedAt
                )
        );

        int safeIdleMinutes = idleMinutes > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) idleMinutes;

        state.setCurrentIdleMinutes(safeIdleMinutes);
        event.setIdleMinutes(safeIdleMinutes);
    }

    private TelematicsEvent toTelematicsEvent(
            Vehicle vehicle,
            GeometrisPacket packet
    ) {
        return TelematicsEvent.builder()
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
//                .idleMinutes(secondsToMinutes(packet.totalIdleDurationSeconds()))
                .idleMinutes(0)
                .harshBraking("HARDBRAKE".equalsIgnoreCase(packet.reasonText()))
                .build();
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
}