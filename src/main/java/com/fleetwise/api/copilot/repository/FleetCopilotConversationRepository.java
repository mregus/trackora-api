package com.fleetwise.api.copilot.repository;

import com.fleetwise.api.copilot.entity.FleetCopilotConversation;
import com.fleetwise.api.copilot.entity.FleetCopilotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FleetCopilotConversationRepository
        extends JpaRepository<FleetCopilotConversation, UUID> {

    List<FleetCopilotConversation>
    findByFleetIdAndUserIdOrderByUpdatedAtDesc(
            UUID fleetId,
            UUID userId
    );

    Optional<FleetCopilotConversation>
    findByIdAndFleetIdAndUserId(
            UUID conversationId,
            UUID fleetId,
            UUID userId
    );
}
