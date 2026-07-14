package com.fleetwise.api.telematics.service;

import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.telematics.dto.PageResponse;
import com.fleetwise.api.telematics.dto.TelematicsTripResponse;
import com.fleetwise.api.telematics.dto.TripResponse;
import com.fleetwise.api.telematics.entity.TelematicsEvent;
import com.fleetwise.api.telematics.repository.TelematicsEventRepository;
import com.fleetwise.api.vehicle.entity.Vehicle;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TripQueryService {

    private static final int TRIP_GAP_MINUTES = 15;

    private final VehicleRepository vehicleRepository;
    private final TelematicsEventRepository telematicsEventRepository;
    private final FleetAccessService fleetAccessService;

    @Transactional(readOnly = true)
    public PageResponse<TelematicsTripResponse> getVehicleTrips(
            UUID userId,
            UUID vehicleId,
            Instant start,
            Instant end,
            int gapMinutes,
            int page,
            int size
    ) {
        Vehicle vehicle = vehicleRepository
                .findByIdAndFleetOwnerId(vehicleId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vehicle not found")
                );

        fleetAccessService.validateAccess(
                vehicle.getFleet().getId(),
                userId
        );

        return buildPagedTrips(
                vehicle,
                start,
                end,
                gapMinutes,
                page,
                size
        );
    }

    @Transactional(readOnly = true)
    public List<TelematicsTripResponse> getVehicleTripsForSystem(
            UUID vehicleId,
            UUID fleetId,
            Instant start,
            Instant end,
            int gapMinutes
    ) {
        Vehicle vehicle = vehicleRepository
                .findByIdAndFleetId(vehicleId, fleetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found in this fleet"
                        )
                );

        List<TelematicsEvent> events =
                telematicsEventRepository
                        .findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                vehicleId,
                                start,
                                end
                        );

        return buildTrips(vehicle, events, gapMinutes);
    }

    private List<TelematicsTripResponse> buildTrips(
            Vehicle vehicle,
            List<TelematicsEvent> events,
            int gapMinutes
    ) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<TelematicsTripResponse> trips = new ArrayList<>();
        List<TelematicsEvent> currentTrip = new ArrayList<>();

        for (TelematicsEvent event : events) {
            if (event.getRecordedAt() == null) {
                continue;
            }

            if (!currentTrip.isEmpty()) {
                TelematicsEvent previous =
                        currentTrip.get(currentTrip.size() - 1);

                long currentGapMinutes = ChronoUnit.MINUTES.between(
                        previous.getRecordedAt(),
                        event.getRecordedAt()
                );

                if (currentGapMinutes >= gapMinutes) {
                    addTripIfValid(vehicle, currentTrip, trips);
                    currentTrip = new ArrayList<>();
                }
            }

            currentTrip.add(event);
        }

        addTripIfValid(vehicle, currentTrip, trips);

        return trips.stream()
                .sorted(
                        Comparator.comparing(
                                TelematicsTripResponse::startTime
                        ).reversed()
                )
                .toList();
    }

    private PageResponse<TelematicsTripResponse> buildPagedTrips(
            Vehicle vehicle,
            Instant start,
            Instant end,
            int gapMinutes,
            int page,
            int size
    ) {
        validateTripRequest(start, end, gapMinutes, page, size);

        List<TelematicsEvent> events =
                telematicsEventRepository
                        .findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                vehicle.getId(),
                                start,
                                end
                        );

        List<TelematicsTripResponse> allTrips =
                buildTrips(vehicle, events, gapMinutes);

        int totalElements = allTrips.size();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / size);

        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<TelematicsTripResponse> content =
                allTrips.subList(fromIndex, toIndex);

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    private java.math.BigDecimal calculateDistance(
            List<TelematicsEvent> points
    ) {
        List<TelematicsEvent> odometerPoints = points.stream()
                .filter(point -> point.getOdometerMiles() != null)
                .toList();

        if (odometerPoints.size() < 2) {
            return java.math.BigDecimal.ZERO;
        }

        var first = odometerPoints.get(0).getOdometerMiles();
        var last = odometerPoints
                .get(odometerPoints.size() - 1)
                .getOdometerMiles();

        var distance = last.subtract(first);

        return distance.signum() < 0
                ? java.math.BigDecimal.ZERO
                : distance;
    }

    private BigDecimal calculateAverageSpeed(
            List<TelematicsEvent> points
    ) {
        List<BigDecimal> speeds = points.stream()
                .map(TelematicsEvent::getSpeedMph)
                .filter(Objects::nonNull)
                .toList();

        if (speeds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = speeds.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(
                BigDecimal.valueOf(speeds.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    private java.math.BigDecimal calculateMaxSpeed(
            List<TelematicsEvent> points
    ) {
        return points.stream()
                .map(TelematicsEvent::getSpeedMph)
                .filter(java.util.Objects::nonNull)
                .max(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ZERO);
    }

    private void addTripIfValid(
            Vehicle vehicle,
            List<TelematicsEvent> points,
            List<TelematicsTripResponse> trips
    ) {
        if (points.size() < 2) {
            return;
        }

        TelematicsEvent first = points.get(0);
        TelematicsEvent last = points.get(points.size() - 1);

        long durationMinutes = Math.max(
                1,
                ChronoUnit.MINUTES.between(
                        first.getRecordedAt(),
                        last.getRecordedAt()
                )
        );

        BigDecimal distanceMiles = calculateDistance(points);
        BigDecimal avgSpeedMph = calculateAverageSpeed(points);
        BigDecimal maxSpeedMph = calculateMaxSpeed(points);

        trips.add(
                new TelematicsTripResponse(
                        first.getRecordedAt(),
                        last.getRecordedAt(),
                        points.size(),
                        maxSpeedMph,
                        avgSpeedMph,
                        durationMinutes,
                        distanceMiles
                )
        );
    }

    private void validateTripRequest(
            Instant start,
            Instant end,
            int gapMinutes,
            int page,
            int size
    ) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start and end timestamps are required"
            );
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "Start timestamp must be before end timestamp"
            );
        }

        if (gapMinutes < 1) {
            throw new IllegalArgumentException(
                    "Gap minutes must be at least 1"
            );
        }

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must not be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }
}