package com.fleetwise.api.copilot.repository;

import com.fleetwise.api.copilot.entity.FleetCopilotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FleetCopilotMessageRepository
        extends JpaRepository<FleetCopilotMessage, UUID> {

    List<FleetCopilotMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<FleetCopilotMessage> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
