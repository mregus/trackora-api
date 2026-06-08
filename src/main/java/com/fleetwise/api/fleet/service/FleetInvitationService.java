package com.fleetwise.api.fleet.service;

import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ForbiddenException;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.fleet.dto.CreateFleetInvitationRequest;
import com.fleetwise.api.fleet.dto.FleetInvitationResponse;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.entity.FleetInvitation;
import com.fleetwise.api.fleet.entity.FleetMember;
import com.fleetwise.api.fleet.repository.FleetInvitationRepository;
import com.fleetwise.api.fleet.repository.FleetMemberRepository;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetInvitationService {

    private final FleetRepository fleetRepository;
    private final FleetInvitationRepository invitationRepository;
    private final FleetMemberRepository fleetMemberRepository;
    private final UserRepository userRepository;
    private final FleetAccessService fleetAccessService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public List<FleetInvitationResponse> listInvitations(
            UUID fleetId,
            UUID requesterId
    ) {
        fleetAccessService.validateWriteAccess(fleetId, requesterId);

        return invitationRepository.findByFleetId(fleetId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FleetInvitationResponse createInvitation(
            UUID fleetId,
            UUID requesterId,
            CreateFleetInvitationRequest request
    ) {
        fleetAccessService.validateWriteAccess(fleetId, requesterId);

        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        String email = request.email().trim().toLowerCase();

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (fleetMemberRepository.existsByFleetIdAndUserId(fleetId, user.getId())) {
                throw new IllegalArgumentException("User is already a member of this fleet");
            }
        });

        if (invitationRepository.existsByFleetIdAndEmailAndAcceptedFalse(fleetId, email)) {
            throw new IllegalArgumentException("An active invitation already exists for this email");
        }

        FleetInvitation invitation = FleetInvitation.builder()
                .fleet(fleet)
                .email(email)
                .role(request.role())
                .token(UUID.randomUUID().toString())
                .accepted(false)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        FleetInvitation saved = invitationRepository.save(invitation);

        sendInvitationEmail(saved);

        return toResponse(saved);
    }

    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        FleetInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invitation token"));

        if (invitation.isAccepted()) {
            throw new IllegalArgumentException("Invitation has already been accepted");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invitation has expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ForbiddenException("This invitation does not belong to your account");
        }

        UUID fleetId = invitation.getFleet().getId();

        if (!fleetMemberRepository.existsByFleetIdAndUserId(fleetId, user.getId())) {
            FleetMember member = FleetMember.builder()
                    .fleet(invitation.getFleet())
                    .user(user)
                    .role(invitation.getRole())
                    .build();

            fleetMemberRepository.save(member);
        }

        invitation.setAccepted(true);
    }

    @Transactional
    public void cancelInvitation(
            UUID fleetId,
            UUID requesterId,
            UUID invitationId
    ) {
        fleetAccessService.validateWriteAccess(fleetId, requesterId);

        FleetInvitation invitation = invitationRepository
                .findByIdAndFleetId(invitationId, fleetId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.isAccepted()) {
            throw new IllegalArgumentException("Accepted invitations cannot be cancelled");
        }

        invitationRepository.delete(invitation);
    }

    @Transactional
    public FleetInvitationResponse resendInvitation(
            UUID fleetId,
            UUID requesterId,
            UUID invitationId
    ) {
        fleetAccessService.validateWriteAccess(fleetId, requesterId);

        FleetInvitation invitation = invitationRepository
                .findByIdAndFleetId(invitationId, fleetId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.isAccepted()) {
            throw new IllegalArgumentException("Accepted invitations cannot be resent");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setToken(UUID.randomUUID().toString());
            invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        }

        sendInvitationEmail(invitation);

        return toResponse(invitation);
    }

    private void sendInvitationEmail(FleetInvitation invitation) {
        String inviteUrl = frontendUrl + "/accept-invitation?token=" + invitation.getToken();

        emailService.sendEmail(
                invitation.getEmail(),
                "You're invited to join Trackora",
                """
                <div style="font-family: Arial, sans-serif; color: #0f172a;">
                  <h2>You have been invited to Trackora</h2>

                  <p>
                    You have been invited to join the fleet:
                    <strong>%s</strong>
                  </p>

                  <p>
                    Role:
                    <strong>%s</strong>
                  </p>

                  <p>
                    <a href="%s"
                       style="display:inline-block;background:#2563eb;color:white;
                              padding:12px 18px;border-radius:10px;text-decoration:none;">
                      Accept Invitation
                    </a>
                  </p>

                  <p>This invitation expires in 7 days.</p>
                </div>
                """.formatted(
                        invitation.getFleet().getName(),
                        invitation.getRole(),
                        inviteUrl
                )
        );
    }

    private FleetInvitationResponse toResponse(FleetInvitation invitation) {
        return new FleetInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.isAccepted(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}