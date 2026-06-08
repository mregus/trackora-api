package com.fleetwise.api.fleet.service;

import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ForbiddenException;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.dto.AddFleetMemberRequest;
import com.fleetwise.api.fleet.dto.FleetMemberResponse;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.entity.FleetMember;
import com.fleetwise.api.fleet.entity.FleetMemberRole;
import com.fleetwise.api.fleet.repository.FleetMemberRepository;
import com.fleetwise.api.fleet.repository.FleetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FleetMemberService {

    private final FleetRepository fleetRepository;
    private final FleetMemberRepository fleetMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FleetMemberResponse> listMembers(UUID fleetId, UUID requesterId) {
        Fleet fleet = getOwnedFleet(fleetId, requesterId);

        return fleetMemberRepository.findByFleetIdOrderByCreatedAtAsc(fleet.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FleetMemberResponse addMember(
            UUID fleetId,
            UUID requesterId,
            AddFleetMemberRequest request
    ) {
        Fleet fleet = getOwnedFleet(fleetId, requesterId);

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (fleetMemberRepository.existsByFleetIdAndUserId(fleetId, user.getId())) {
            throw new IllegalArgumentException("User is already a member of this fleet");
        }

        FleetMember member = FleetMember.builder()
                .fleet(fleet)
                .user(user)
                .role(request.role())
                .build();

        return toResponse(fleetMemberRepository.save(member));
    }

    @Transactional
    public void removeMember(UUID fleetId, UUID requesterId, UUID memberId) {
        Fleet fleet = getOwnedFleet(fleetId, requesterId);

        FleetMember member = fleetMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet member not found"));

        if (!member.getFleet().getId().equals(fleet.getId())) {
            throw new ResourceNotFoundException("Fleet member not found");
        }

        if (member.getRole() == FleetMemberRole.OWNER) {
            throw new ForbiddenException("Cannot remove fleet owner");
        }

        fleetMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public boolean hasFleetAccess(UUID fleetId, UUID userId) {

        boolean owner = fleetRepository
                .findByIdAndOwnerId(fleetId, userId)
                .isPresent();

        if (owner) {
            return true;
        }

        return fleetMemberRepository
                .existsByFleetIdAndUserId(fleetId, userId);
    }

    @Transactional(readOnly = true)
    public void validateFleetAccess(
            UUID fleetId,
            UUID userId
    ) {
        if (!hasFleetAccess(fleetId, userId)) {
            throw new ForbiddenException(
                    "You do not have access to this fleet"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> getAccessibleFleetIds(UUID userId) {

        List<UUID> fleetIds = fleetRepository.findByOwnerId(userId)
                .stream()
                .map(Fleet::getId)
                .toList();

        List<UUID> memberFleetIds =
                fleetMemberRepository.findFleetIdsByUserId(userId);

        Set<UUID> allFleetIds = new HashSet<>();

        allFleetIds.addAll(fleetIds);
        allFleetIds.addAll(memberFleetIds);

        return new ArrayList<>(allFleetIds);
    }

    private Fleet getOwnedFleet(UUID fleetId, UUID requesterId) {
        return fleetRepository.findByIdAndOwnerId(fleetId, requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));
    }

    private FleetMemberResponse toResponse(FleetMember member) {
        User user = member.getUser();

        return new FleetMemberResponse(
                member.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}