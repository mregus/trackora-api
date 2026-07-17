package com.fleetwise.api.telematics.service;

import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.dashboard.DashboardRealtimePublisher;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.notification.service.NotificationService;
import com.fleetwise.api.safety.service.SafetyScoreUpdaterService;
import com.fleetwise.api.safety.websocket.SafetyScoreRealtimePublisher;
import com.fleetwise.api.telematics.entity.TelematicsEvent;
import com.fleetwise.api.telematics.entity.VehicleCurrentState;
import com.fleetwise.api.telematics.geometris.GeometrisGpsTrailDecoder;
import com.fleetwise.api.telematics.geometris.GeometrisPacket;
import com.fleetwise.api.telematics.geometris.GeometrisPacketParser;
import com.fleetwise.api.telematics.geometris.GeometrisRawPacketRepository;
import com.fleetwise.api.telematics.observability.TelematicsMetrics;
import com.fleetwise.api.telematics.repository.RawTelematicsPacketRepository;
import com.fleetwise.api.telematics.repository.TelematicsDeviceRepository;
import com.fleetwise.api.telematics.repository.TelematicsEventRepository;
import com.fleetwise.api.telematics.repository.VehicleCurrentStateRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelematicsServiceIdleStateTest {

    @Mock
    private GeometrisGpsTrailDecoder gpsTrailDecoder;

    @Mock
    private TelematicsDeviceRepository telematicsDeviceRepository;

    @Mock
    private TelematicsEventRepository telematicsEventRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private FleetAccessService fleetAccessService;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GeometrisPacketParser geometrisPacketParser;

    @Mock
    private GeometrisRawPacketRepository geometrisRawPacketRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RawTelematicsPacketRepository rawTelematicsPacketRepository;

    @Mock
    private TelematicsMetrics telematicsMetrics;

    @Mock
    private VehicleCurrentStateRepository vehicleCurrentStateRepository;

    @Mock
    private DashboardRealtimePublisher dashboardRealtimePublisher;

    @Mock
    private SafetyScoreUpdaterService safetyScoreUpdaterService;

    @Mock
    private SafetyScoreRealtimePublisher safetyScoreRealtimePublisher;

    @InjectMocks
    private TelematicsService service;

    @Test
    void shouldCreateCurrentStateWhenVehicleHasNoExistingState() {
        Vehicle vehicle = vehicle();

        when(vehicleCurrentStateRepository.findById(vehicle.getId()))
                .thenReturn(Optional.empty());

        VehicleCurrentState state = invokeGetOrCreateCurrentState(vehicle);

        assertThat(state).isNotNull();
        assertThat(state.getVehicle()).isEqualTo(vehicle);
        assertThat(state.getCurrentIdleMinutes()).isZero();

        verify(vehicleCurrentStateRepository)
                .findById(vehicle.getId());
    }

    @Test
    void shouldReturnExistingCurrentState() {
        Vehicle vehicle = vehicle();

        VehicleCurrentState existing = new VehicleCurrentState();
        existing.setVehicle(vehicle);
        existing.setCurrentIdleMinutes(12);

        when(vehicleCurrentStateRepository.findById(vehicle.getId()))
                .thenReturn(Optional.of(existing));

        VehicleCurrentState result =
                invokeGetOrCreateCurrentState(vehicle);

        assertThat(result).isSameAs(existing);
        assertThat(result.getCurrentIdleMinutes()).isEqualTo(12);
    }

    @Test
    void shouldStartIdleSessionWhenIgnitionIsOnAndSpeedIsZero() {
        Instant recordedAt =
                Instant.parse("2026-07-15T20:00:00Z");

        VehicleCurrentState state = new VehicleCurrentState();

        TelematicsEvent event = event(
                recordedAt,
                BigDecimal.ZERO
        );

        GeometrisPacket packet = mockPacket(true);

        invokeUpdateEventAndIdleState(
                state,
                event,
                packet
        );

        assertThat(state.getIgnitionOn()).isTrue();
        assertThat(state.getIdleStartedAt())
                .isEqualTo(recordedAt);
        assertThat(state.getCurrentIdleMinutes()).isZero();
        assertThat(event.getIdleMinutes()).isZero();
        assertThat(state.getLastSeenAt()).isEqualTo(recordedAt);
    }

    @Test
    void shouldTreatSpeedBelowThresholdAsIdling() {
        Instant recordedAt =
                Instant.parse("2026-07-15T20:00:00Z");

        VehicleCurrentState state = new VehicleCurrentState();

        TelematicsEvent event = event(
                recordedAt,
                BigDecimal.valueOf(1.5)
        );

        invokeUpdateEventAndIdleState(
                state,
                event,
                mockPacket(true)
        );

        assertThat(state.getIdleStartedAt())
                .isEqualTo(recordedAt);
        assertThat(state.getCurrentIdleMinutes()).isZero();
    }

    @Test
    void shouldCalculateElapsedIdleMinutes() {
        VehicleCurrentState state = new VehicleCurrentState();
        state.setIgnitionOn(true);
        state.setIdleStartedAt(
                Instant.parse("2026-07-15T20:00:00Z")
        );

        TelematicsEvent event = event(
                Instant.parse("2026-07-15T20:36:30Z"),
                BigDecimal.ZERO
        );

        /*
         * Passing null represents a packet such as 5873 that does not
         * contain ignition. The previously known ignition state is retained.
         */
        invokeUpdateEventAndIdleState(
                state,
                event,
                null
        );

        assertThat(state.getIgnitionOn()).isTrue();
        assertThat(state.getIdleStartedAt())
                .isEqualTo(
                        Instant.parse("2026-07-15T20:00:00Z")
                );
        assertThat(state.getCurrentIdleMinutes()).isEqualTo(36);
        assertThat(event.getIdleMinutes()).isEqualTo(36);
    }

    @Test
    void shouldContinueIdleSessionWhenPacketDoesNotContainIgnition() {
        VehicleCurrentState state = new VehicleCurrentState();
        state.setIgnitionOn(true);
        state.setIdleStartedAt(
                Instant.parse("2026-07-15T20:00:00Z")
        );

        GeometrisPacket packet = packetWithIgnition(null);

        when(packet.ignitionOn()).thenReturn(null);
        when(packet.activeDtc()).thenReturn(null);

        TelematicsEvent event = event(
                Instant.parse("2026-07-15T20:15:00Z"),
                BigDecimal.ZERO
        );

        invokeUpdateEventAndIdleState(
                state,
                event,
                packet
        );

        assertThat(state.getIgnitionOn()).isTrue();
        assertThat(state.getCurrentIdleMinutes()).isEqualTo(15);
        assertThat(event.getIdleMinutes()).isEqualTo(15);
    }

    @Test
    void shouldClearIdleSessionWhenVehicleMoves() {
        VehicleCurrentState state = new VehicleCurrentState();
        state.setIgnitionOn(true);
        state.setIdleStartedAt(
                Instant.parse("2026-07-15T20:00:00Z")
        );
        state.setCurrentIdleMinutes(35);

        TelematicsEvent event = event(
                Instant.parse("2026-07-15T20:36:00Z"),
                BigDecimal.valueOf(12)
        );

        invokeUpdateEventAndIdleState(
                state,
                event,
                null
        );

        assertThat(state.getIdleStartedAt()).isNull();
        assertThat(state.getCurrentIdleMinutes()).isZero();
        assertThat(event.getIdleMinutes()).isZero();
    }

    @Test
    void shouldClearIdleSessionWhenIgnitionTurnsOff() {
        VehicleCurrentState state = new VehicleCurrentState();
        state.setIgnitionOn(true);
        state.setIdleStartedAt(
                Instant.parse("2026-07-15T20:00:00Z")
        );
        state.setCurrentIdleMinutes(20);

        TelematicsEvent event = event(
                Instant.parse("2026-07-15T20:21:00Z"),
                BigDecimal.ZERO
        );

        invokeUpdateEventAndIdleState(
                state,
                event,
                mockPacket(false)
        );

        assertThat(state.getIgnitionOn()).isFalse();
        assertThat(state.getIdleStartedAt()).isNull();
        assertThat(state.getCurrentIdleMinutes()).isZero();
        assertThat(event.getIdleMinutes()).isZero();
    }

    @Test
    void shouldNotTreatUnknownIgnitionAsIdling() {
        VehicleCurrentState state = new VehicleCurrentState();

        TelematicsEvent event = event(
                Instant.parse("2026-07-15T20:00:00Z"),
                BigDecimal.ZERO
        );

        invokeUpdateEventAndIdleState(
                state,
                event,
                null
        );

        assertThat(state.getIgnitionOn()).isNull();
        assertThat(state.getIdleStartedAt()).isNull();
        assertThat(state.getCurrentIdleMinutes()).isZero();
        assertThat(event.getIdleMinutes()).isZero();
    }

    @Test
    void shouldNotAllowOlderPacketToOverwriteCurrentState() {
        Instant newestTimestamp =
                Instant.parse("2026-07-15T20:30:00Z");

        VehicleCurrentState state = new VehicleCurrentState();
        state.setLastSeenAt(newestTimestamp);
        state.setLatitude(29.131130);
        state.setLongitude(-82.194075);
        state.setSpeedMph(BigDecimal.ZERO);
        state.setIgnitionOn(true);
        state.setIdleStartedAt(
                Instant.parse("2026-07-15T20:00:00Z")
        );
        state.setCurrentIdleMinutes(30);

        TelematicsEvent olderEvent = TelematicsEvent.builder()
                .recordedAt(
                        Instant.parse("2026-07-15T20:20:00Z")
                )
                .latitude(30.000000)
                .longitude(-81.000000)
                .speedMph(BigDecimal.valueOf(25))
                .idleMinutes(0)
                .build();

        GeometrisPacket packet = mock(GeometrisPacket.class);

        invokeUpdateEventAndIdleState(
                state,
                olderEvent,
                packet
        );

        assertThat(state.getLastSeenAt())
                .isEqualTo(newestTimestamp);

        assertThat(state.getLatitude())
                .isEqualTo(29.131130);

        assertThat(state.getLongitude())
                .isEqualTo(-82.194075);

        assertThat(state.getSpeedMph())
                .isEqualByComparingTo("0");

        assertThat(state.getIgnitionOn()).isTrue();

        assertThat(state.getIdleStartedAt())
                .isEqualTo(
                        Instant.parse("2026-07-15T20:00:00Z")
                );

        assertThat(state.getCurrentIdleMinutes())
                .isEqualTo(30);

        verifyNoInteractions(packet);
    }

    @Test
    void shouldMergeAvailableEventFieldsIntoCurrentState() {
        VehicleCurrentState state = new VehicleCurrentState();
        state.setFuelLevelPercent(BigDecimal.valueOf(55));

        TelematicsEvent event = TelematicsEvent.builder()
                .recordedAt(
                        Instant.parse("2026-07-15T20:00:00Z")
                )
                .latitude(29.131126)
                .longitude(-82.194076)
                .speedMph(BigDecimal.valueOf(14))
                .headingDegrees(222)
                .fuelLevelPercent(BigDecimal.valueOf(61))
                .checkEngine(true)
                .idleMinutes(0)
                .build();

        GeometrisPacket packet = packetWithIgnition(true);
        when(packet.ignitionOn()).thenReturn(true);
        when(packet.activeDtc()).thenReturn("1:1:P0130");

        invokeUpdateEventAndIdleState(
                state,
                event,
                packet
        );

        assertThat(state.getLatitude()).isEqualTo(29.131126);
        assertThat(state.getLongitude()).isEqualTo(-82.194076);
        assertThat(state.getSpeedMph())
                .isEqualByComparingTo("14");
        assertThat(state.getHeadingDegrees()).isEqualTo(222);
        assertThat(state.getFuelLevelPercent())
                .isEqualByComparingTo("61");
        assertThat(state.isCheckEngine()).isTrue();
    }

    @Test
    void shouldNotEraseCheckEngineWhenPacketHasNoDtcField() {
        VehicleCurrentState state = new VehicleCurrentState();
        state.setCheckEngine(true);

        TelematicsEvent event = event(
                Instant.parse("2026-07-15T20:00:00Z"),
                BigDecimal.valueOf(10)
        );

        event.setCheckEngine(false);

        GeometrisPacket packet = packetWithIgnition(null);
        when(packet.activeDtc()).thenReturn(null);
        when(packet.ignitionOn()).thenReturn(null);

        invokeUpdateEventAndIdleState(
                state,
                event,
                packet
        );

        assertThat(state.isCheckEngine()).isTrue();
    }

    private Vehicle vehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(UUID.randomUUID());
        return vehicle;
    }

    private TelematicsEvent event(
            Instant recordedAt,
            BigDecimal speedMph
    ) {
        return TelematicsEvent.builder()
                .recordedAt(recordedAt)
                .speedMph(speedMph)
                .idleMinutes(0)
                .build();
    }

    private GeometrisPacket mockPacket(boolean ignitionOn) {
        GeometrisPacket packet = mock(GeometrisPacket.class);

        when(packet.ignitionOn()).thenReturn(ignitionOn);
        when(packet.activeDtc()).thenReturn(null);

        return packet;
    }

    private GeometrisPacket packetWithIgnition(
            Boolean ignitionOn
    ) {
        GeometrisPacket packet =
                mock(GeometrisPacket.class);

        when(packet.ignitionOn())
                .thenReturn(ignitionOn);

        return packet;
    }

    private VehicleCurrentState invokeGetOrCreateCurrentState(
            Vehicle vehicle
    ) {
        return ReflectionTestUtils.invokeMethod(
                service,
                "getOrCreateCurrentState",
                vehicle
        );
    }

    private void invokeUpdateEventAndIdleState(
            VehicleCurrentState state,
            TelematicsEvent event,
            GeometrisPacket packet
    ) {
        ReflectionTestUtils.invokeMethod(
                service,
                "updateEventAndIdleState",
                state,
                event,
                packet
        );
    }
}