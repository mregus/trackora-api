package com.fleetwise.api.fleet.service;

import com.fleetwise.api.common.exception.ForbiddenException;
import com.fleetwise.api.fleet.entity.FleetMemberRole;
import com.fleetwise.api.fleet.repository.FleetMemberRepository;
import com.fleetwise.api.fleet.repository.FleetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetAccessService {

    private final FleetRepository fleetRepository;
    private final FleetMemberRepository fleetMemberRepository;

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID fleetId, UUID userId) {
        return fleetRepository.findByIdAndOwnerId(fleetId, userId).isPresent()
                || fleetMemberRepository.existsByFleetIdAndUserId(fleetId, userId);
    }

    @Transactional(readOnly = true)
    public void validateAccess(UUID fleetId, UUID userId) {
        if (!hasAccess(fleetId, userId)) {
            throw new ForbiddenException("You do not have access to this fleet");
        }
    }

    @Transactional(readOnly = true)
    public void validateWriteAccess(UUID fleetId, UUID userId) {
        boolean owner = fleetRepository.findByIdAndOwnerId(fleetId, userId).isPresent();

        if (owner) {
            return;
        }

        boolean manager = fleetMemberRepository
                .findByFleetIdAndUserId(fleetId, userId)
                .map(member -> member.getRole() == FleetMemberRole.MANAGER)
                .orElse(false);

        if (!manager) {
            throw new ForbiddenException("You do not have permission to modify this fleet");
        }
    }
}