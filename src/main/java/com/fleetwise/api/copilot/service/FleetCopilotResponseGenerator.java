package com.fleetwise.api.copilot.service;

import com.fleetwise.api.copilot.ai.FleetCopilotOpenAiClient;
import com.fleetwise.api.copilot.dto.CopilotConversationMessage;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.entity.FleetCopilotConversation;
import com.fleetwise.api.copilot.observability.CopilotMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FleetCopilotResponseGenerator {

    private final ObjectProvider<FleetCopilotOpenAiClient> openAiClientProvider;
    private final CopilotMetrics copilotMetrics;

    public FleetCopilotResponse generate(
            UUID userId,
            UUID fleetId,
            FleetCopilotConversation conversation,
            String question,
            List<CopilotConversationMessage> history,
            FleetCopilotResponse fallback
    ) {
        FleetCopilotOpenAiClient client =
                openAiClientProvider.getIfAvailable();

        if (client == null) {
            copilotMetrics.fallbackUsed("ai_disabled");

            return fallbackResponse(
                    conversation.getId(),
                    fallback
            );
        }

        try {
            String aiAnswer = copilotMetrics.recordAiCall(
                    () -> client.answerWithTools(
                            fleetId,
                            userId,
                            question,
                            history
                    )
            );

            return new FleetCopilotResponse(
                    conversation.getId(),
                    aiAnswer,
                    fallback.supportingFacts(),
                    Instant.now(),
                    true
            );

        } catch (Exception ex) {
            String failureReason = classifyFailure(ex);

            copilotMetrics.fallbackUsed(failureReason);

            log.warn(
                    "Fleet Copilot AI flow failed fleetId={}, conversationId={}, reason={}, error={}",
                    fleetId,
                    conversation.getId(),
                    failureReason,
                    ex.getMessage()
            );

            return fallbackResponse(
                    conversation.getId(),
                    fallback
            );
        }
    }

    private FleetCopilotResponse fallbackResponse(
            UUID conversationId,
            FleetCopilotResponse fallback
    ) {
        return new FleetCopilotResponse(
                conversationId,
                fallback.answer(),
                fallback.supportingFacts(),
                Instant.now(),
                false
        );
    }

    private String classifyFailure(Exception ex) {
        Throwable root = rootCause(ex);
        String type = root.getClass().getSimpleName();

        if (type.contains("HttpClientError")) {
            return "openai_client_error";
        }

        if (type.contains("HttpServerError")) {
            return "openai_server_error";
        }

        if (type.contains("ResourceAccess")) {
            return "openai_connection_error";
        }

        if (root instanceof IllegalArgumentException) {
            return "invalid_request";
        }

        if (root instanceof IllegalStateException) {
            return "invalid_ai_response";
        }

        return "unknown";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }
}