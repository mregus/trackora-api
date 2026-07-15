package com.fleetwise.api.copilot.service;

import com.fleetwise.api.copilot.conversation.dto.CopilotConversationMessage;
import com.fleetwise.api.copilot.conversation.service.FleetCopilotConversationService;
import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.conversation.entity.FleetCopilotConversation;
import com.fleetwise.api.copilot.fallback.FleetCopilotFallbackService;
import com.fleetwise.api.copilot.observability.CopilotMetrics;
import com.fleetwise.api.fleet.service.FleetAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FleetCopilotService {

    private final FleetCopilotFallbackService fallbackService;
    private final FleetAccessService fleetAccessService;
    private final FleetCopilotContextService contextService;
    private final FleetCopilotConversationService conversationService;
    private final FleetCopilotResponseGenerator responseGenerator;
    private final CopilotMetrics copilotMetrics;

    public FleetCopilotResponse ask(
            UUID userId,
            UUID fleetId,
            UUID conversationId,
            String question
    ) {
        copilotMetrics.requestStarted();

        fleetAccessService.validateAccess(fleetId, userId);

        FleetCopilotConversation conversation =
                conversationService.getOrCreate(
                        conversationId,
                        fleetId,
                        userId,
                        question
                );

        List<CopilotConversationMessage> history =
                conversationService.getRecentConversationHistory(
                        conversation.getId()
                );

        conversationService.saveUserMessage(
                conversation,
                question
        );

        FleetCopilotContext context =
                contextService.build(fleetId);

        FleetCopilotResponse fallback =
                fallbackService.build(
                        conversation.getId(),
                        context,
                        question
                );

        FleetCopilotResponse response =
                responseGenerator.generate(
                        userId,
                        fleetId,
                        conversation,
                        question,
                        history,
                        fallback
                );

        conversationService.saveAssistantMessage(
                conversation,
                response
        );

        copilotMetrics.requestCompleted(
                response.aiGenerated()
        );

        return response;
    }

    private String classifyCopilotFailure(Exception ex) {
        String name = ex.getClass().getSimpleName();

        if (name.contains("HttpClientError")) {
            return "openai_client_error";
        }

        if (name.contains("HttpServerError")) {
            return "openai_server_error";
        }

        if (name.contains("ResourceAccess")) {
            return "openai_connection_error";
        }

        if (ex instanceof IllegalArgumentException) {
            return "invalid_request";
        }

        return "unknown";
    }
}