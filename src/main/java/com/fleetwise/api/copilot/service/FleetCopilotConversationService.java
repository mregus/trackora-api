package com.fleetwise.api.copilot.service;

import com.fleetwise.api.auth.entity.User;
import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.copilot.dto.*;
import com.fleetwise.api.copilot.entity.CopilotMessageRole;
import com.fleetwise.api.copilot.entity.FleetCopilotConversation;
import com.fleetwise.api.copilot.entity.FleetCopilotMessage;
import com.fleetwise.api.copilot.repository.FleetCopilotConversationRepository;
import com.fleetwise.api.copilot.repository.FleetCopilotMessageRepository;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetCopilotConversationService {

    private final FleetRepository fleetRepository;
    private final UserRepository userRepository;
    private final FleetCopilotConversationRepository conversationRepository;
    private final FleetCopilotMessageRepository messageRepository;

    @Transactional
    public FleetCopilotConversation getOrCreate(
            UUID conversationId,
            UUID fleetId,
            UUID userId,
            String firstQuestion
    ) {
        if (conversationId != null) {
            return conversationRepository
                    .findByIdAndFleetIdAndUserId(
                            conversationId,
                            fleetId,
                            userId
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Copilot conversation not found"
                            )
                    );
        }

        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Fleet not found")
                );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return conversationRepository.save(
                FleetCopilotConversation.builder()
                        .fleet(fleet)
                        .user(user)
                        .title(createTitle(firstQuestion))
                        .build()
        );
    }

    @Transactional
    public void saveUserMessage(
            FleetCopilotConversation conversation,
            String question
    ) {
        saveMessage(
                conversation,
                CopilotMessageRole.USER,
                question,
                null,
                false
        );
    }

    @Transactional
    public void saveAssistantMessage(
            FleetCopilotConversation conversation,
            FleetCopilotResponse response
    ) {
        String facts = response.supportingFacts() == null
                ? null
                : String.join("\n", response.supportingFacts());

        saveMessage(
                conversation,
                CopilotMessageRole.ASSISTANT,
                response.answer(),
                facts,
                response.aiGenerated()
        );
    }

    @Transactional(readOnly = true)
    public List<CopilotConversationMessage> getConversationHistory(
            UUID conversationId
    ) {
        return messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(message -> new CopilotConversationMessage(
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CopilotConversationMessage> getRecentConversationHistory(
            UUID conversationId
    ) {
        List<FleetCopilotMessage> messages = new ArrayList<>(
                messageRepository
                        .findTop20ByConversationIdOrderByCreatedAtDesc(
                                conversationId
                        )
        );

        Collections.reverse(messages);

        return messages.stream()
                .map(message -> new CopilotConversationMessage(
                        message.getRole(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CopilotConversationDetailResponse getConversation(
            UUID conversationId,
            UUID fleetId,
            UUID userId
    ) {
        FleetCopilotConversation conversation = conversationRepository
                .findByIdAndFleetIdAndUserId(
                        conversationId,
                        fleetId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Copilot conversation not found"
                        )
                );

        List<CopilotMessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return new CopilotConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    @Transactional(readOnly = true)
    public List<CopilotConversationSummaryResponse> getConversations(
            UUID fleetId,
            UUID userId
    ) {
        return conversationRepository
                .findByFleetIdAndUserIdOrderByUpdatedAtDesc(fleetId, userId)
                .stream()
                .map(conversation -> new CopilotConversationSummaryResponse(
                        conversation.getId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public CopilotConversationSummaryResponse renameConversation(
            UUID conversationId,
            UUID fleetId,
            UUID userId,
            String title
    ) {
        FleetCopilotConversation conversation = conversationRepository
                .findByIdAndFleetIdAndUserId(
                        conversationId,
                        fleetId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Copilot conversation not found"
                        )
                );

        conversation.setTitle(title.trim());

        FleetCopilotConversation saved =
                conversationRepository.save(conversation);

        return new CopilotConversationSummaryResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteConversation(
            UUID conversationId,
            UUID fleetId,
            UUID userId
    ) {
        FleetCopilotConversation conversation = conversationRepository
                .findByIdAndFleetIdAndUserId(
                        conversationId,
                        fleetId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Copilot conversation not found"
                        )
                );

        conversationRepository.delete(conversation);
    }

    private void saveMessage(
            FleetCopilotConversation conversation,
            CopilotMessageRole role,
            String content,
            String supportingFacts,
            boolean aiGenerated
    ) {
        messageRepository.save(
                FleetCopilotMessage.builder()
                        .conversation(conversation)
                        .role(role)
                        .content(content)
                        .supportingFacts(supportingFacts)
                        .aiGenerated(aiGenerated)
                        .build()
        );

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
    }

    private String createTitle(String question) {
        String trimmed = question.trim();

        return trimmed.length() <= 80
                ? trimmed
                : trimmed.substring(0, 77) + "...";
    }

    private CopilotMessageResponse toMessageResponse(
            FleetCopilotMessage message
    ) {
        List<String> facts =
                message.getSupportingFacts() == null
                        || message.getSupportingFacts().isBlank()
                        ? List.of()
                        : message.getSupportingFacts().lines().toList();

        return new CopilotMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                facts,
                message.isAiGenerated(),
                message.getCreatedAt()
        );
    }
}