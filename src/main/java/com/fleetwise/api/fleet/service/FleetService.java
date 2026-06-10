package com.fleetwise.api.fleet.service;

import com.fleetwise.api.activity.entity.ActivityAction;
import com.fleetwise.api.activity.service.ActivityLogService;
import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.dto.CreateFleetRequest;
import com.fleetwise.api.fleet.dto.FleetResponse;
import com.fleetwise.api.fleet.dto.UpdateFleetRequest;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetService {

    private final FleetRepository fleetRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public FleetResponse createFleet(UUID ownerUserId, CreateFleetRequest request) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Fleet fleet = Fleet.builder()
                .name(request.name())
                .owner(owner)
                .build();

        Fleet saved = fleetRepository.save(fleet);

        User user = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        activityLogService.log(
                user,
                saved,
                null,
                ActivityAction.FLEET_CREATED,
                "FLEET",
                saved.getId(),
                "Created fleet %s".formatted(saved.getName())
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FleetResponse> getMyFleets(UUID ownerUserId) {
        return fleetRepository.findByOwnerIdOrderByCreatedAtDesc(ownerUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FleetResponse getFleet(UUID fleetId, UUID ownerUserId) {
        Fleet fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        return toResponse(fleet);
    }

    @Transactional
    public FleetResponse updateFleet(UUID fleetId, UUID ownerUserId, UpdateFleetRequest request) {
        Fleet fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        fleet.setName(request.name());

        return toResponse(fleet);
    }

    @Transactional
    public void deleteFleet(UUID fleetId, UUID ownerUserId) {
        Fleet fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        fleetRepository.delete(fleet);
    }

    private FleetResponse toResponse(Fleet fleet) {
        return new FleetResponse(
                fleet.getId(),
                fleet.getName(),
                fleet.getOwner().getId(),
                fleet.getCreatedAt(),
                fleet.getUpdatedAt()
        );
    }
}