package com.fleetwise.api.activity.service;

import com.fleetwise.api.activity.dto.ActivityLogResponse;
import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.entity.ActivityLog;
import com.fleetwise.api.activity.repository.ActivityLogRepository;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.vehicle.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final FleetAccessService fleetAccessService;

    @Transactional
    public void log(
            User user,
            Fleet fleet,
            Vehicle vehicle,
            ActivityAction action,
            String entityType,
            UUID entityId,
            String message
    ) {

        fleetAccessService.validateWriteAccess(fleet.getId(), user.getId());

        ActivityLog log = ActivityLog.builder()
                .fleet(fleet)
                .vehicle(vehicle)
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .message(message)
                .build();

        activityLogRepository.save(log);
    }

    public void log(
            User user,
            Vehicle vehicle,
            ActivityAction action,
            String entityType,
            UUID entityId,
            String message
    ) {
        log(
                user,
                vehicle != null ? vehicle.getFleet() : null,
                vehicle,
                action,
                entityType,
                entityId,
                message
        );
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponse> getFleetActivity(UUID fleetId, UUID ownerId) {

        fleetAccessService.validateAccess(fleetId, ownerId);

        return activityLogRepository.findTop50ByFleetIdOrderByCreatedAtDesc(fleetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getFleet() != null ? log.getFleet().getId() : null,
                log.getVehicle() != null ? log.getVehicle().getId() : null,
                log.getUser().getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }
}