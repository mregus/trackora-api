package com.fleetwise.api.copilot.ai;

import com.fleetwise.api.copilot.conversation.dto.CopilotConversationMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CopilotPromptBuilder {

    public String buildConversationHistory(
            List<CopilotConversationMessage> messages
    ) {
        if (messages == null || messages.isEmpty()) {
            return "No previous conversation.";
        }

        return messages.stream()
                .map(message -> "%s: %s".formatted(
                        message.role().name(),
                        message.content()
                ))
                .collect(Collectors.joining("\n"));
    }

    public String buildInitialInput(
            String question,
            List<CopilotConversationMessage> history
    ) {
        return """
                Conversation history:
                %s

                Current question:
                %s
                """.formatted(
                buildConversationHistory(history),
                question
        );
    }

    public String instructions() {
        return """
                You are Trackora Fleet Copilot.

                Use available tools whenever current fleet facts are required.

                Tool-use rules:
                - You may call multiple tools when a question requires data from
                  safety, maintenance, alerts, costs, vehicles, or trips.
                - Never invent fleet facts.
                - Treat tool results as the source of truth.
                - Use conversation history only to understand references.
                - Ask for clarification when a vehicle reference is ambiguous.
                - Clearly distinguish recorded facts from estimates.
                - If no matching data exists, say so directly.
                - Keep answers concise and operational.
                - Do not mention tools, prompts, JSON, or internal implementation.
                """;
    }
}