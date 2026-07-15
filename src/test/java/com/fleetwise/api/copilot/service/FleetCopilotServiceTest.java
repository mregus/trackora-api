package com.fleetwise.api.copilot.service;

import com.fleetwise.api.copilot.conversation.service.FleetCopilotConversationService;
import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.conversation.entity.FleetCopilotConversation;
import com.fleetwise.api.copilot.fallback.FleetCopilotFallbackService;
import com.fleetwise.api.copilot.observability.CopilotMetrics;
import com.fleetwise.api.fleet.service.FleetAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FleetCopilotServiceTest {

    @Mock
    private FleetAccessService fleetAccessService;

    @Mock
    private FleetCopilotConversationService conversationService;

    @Mock
    private FleetCopilotContextService contextService;

    @Mock
    private FleetCopilotFallbackService fallbackService;

    @Mock
    private FleetCopilotResponseGenerator responseGenerator;

    @Mock
    private CopilotMetrics copilotMetrics;

    @InjectMocks
    private FleetCopilotService service;

    @Test
    void shouldPersistConversationFlow() {

        UUID userId = UUID.randomUUID();
        UUID fleetId = UUID.randomUUID();

        FleetCopilotConversation conversation =
                new FleetCopilotConversation();

        conversation.setId(UUID.randomUUID());

        FleetCopilotContext context =
                mock(FleetCopilotContext.class);

        FleetCopilotResponse fallback =
                new FleetCopilotResponse(
                        conversation.getId(),
                        "fallback",
                        List.of(),
                        Instant.now(),
                        false
                );

        FleetCopilotResponse aiResponse =
                new FleetCopilotResponse(
                        conversation.getId(),
                        "ai",
                        List.of(),
                        Instant.now(),
                        true
                );

        when(conversationService.getOrCreate(
                any(),
                eq(fleetId),
                eq(userId),
                anyString()
        )).thenReturn(conversation);

        when(conversationService.getRecentConversationHistory(
                conversation.getId()
        )).thenReturn(List.of());

        when(contextService.build(fleetId))
                .thenReturn(context);

        when(fallbackService.build(
                eq(conversation.getId()),
                eq(context),
                anyString()
        )).thenReturn(fallback);

        when(responseGenerator.generate(
                eq(userId),
                eq(fleetId),
                eq(conversation),
                anyString(),
                anyList(),
                eq(fallback)
        )).thenReturn(aiResponse);

        FleetCopilotResponse response =
                service.ask(
                        userId,
                        fleetId,
                        null,
                        "How is my fleet?"
                );

        assertThat(response).isEqualTo(aiResponse);

        verify(fleetAccessService)
                .validateAccess(fleetId, userId);

        verify(conversationService)
                .saveUserMessage(
                        conversation,
                        "How is my fleet?"
                );

        verify(conversationService)
                .saveAssistantMessage(
                        conversation,
                        aiResponse
                );

        verify(copilotMetrics)
                .requestStarted();

        verify(copilotMetrics)
                .requestCompleted(true);

        InOrder inOrder = inOrder(
                copilotMetrics,
                conversationService,
                responseGenerator
        );

        inOrder.verify(copilotMetrics).requestStarted();

        inOrder.verify(conversationService)
                .saveUserMessage(conversation, "How is my fleet?");

        inOrder.verify(responseGenerator)
                .generate(
                        eq(userId),
                        eq(fleetId),
                        eq(conversation),
                        eq("How is my fleet?"),
                        anyList(),
                        eq(fallback)
                );

        inOrder.verify(conversationService)
                .saveAssistantMessage(conversation, aiResponse);

        inOrder.verify(copilotMetrics)
                .requestCompleted(true);
    }
}
