package com.fleetwise.api.copilot.conversation.repository;

import com.fleetwise.api.copilot.conversation.entity.FleetCopilotConversation;
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
