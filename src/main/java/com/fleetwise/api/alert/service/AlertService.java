package com.fleetwise.api.alert.service;

import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.alert.dto.AlertResponse;
import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.repository.FleetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final FleetRepository fleetRepository;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AlertResponse> getFleetAlerts(UUID fleetId, UUID ownerId) {
        fleetRepository.findByIdAndOwnerId(fleetId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        return alertRepository.findByFleetIdOrderByCreatedAtDesc(fleetId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AlertResponse resolve(UUID alertId, UUID ownerId) {
        Alert alert = alertRepository.findByIdAndFleetOwnerId(alertId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found or not authorized"));
        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        activityLogService.log(
                user,
                alert.getVehicle(),
                ActivityAction.ALERT_RESOLVED,
                "ALERT",
                alert.getId(),
                "Resolved alert %s".formatted(alert.getType())
        );

        return toResponse(alert);
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getFleet().getId(),
                alert.getVehicle() != null ? alert.getVehicle().getId() : null,
                alert.getType(),
                alert.getMessage(),
                alert.isResolved(),
                alert.getSeverity(),
                alert.getCreatedAt(),
                alert.getResolvedAt()
        );
    }
}
