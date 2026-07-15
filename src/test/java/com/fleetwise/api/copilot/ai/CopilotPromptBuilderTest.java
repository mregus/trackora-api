package com.fleetwise.api.copilot.ai;

import com.fleetwise.api.copilot.conversation.dto.CopilotConversationMessage;
import com.fleetwise.api.copilot.entity.CopilotMessageRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CopilotPromptBuilderTest {

    private final CopilotPromptBuilder builder =
            new CopilotPromptBuilder();

    @Test
    void shouldReturnDefaultWhenHistoryIsEmpty() {
        assertThat(
                builder.buildConversationHistory(List.of())
        ).isEqualTo("No previous conversation.");
    }

    @Test
    void shouldFormatConversationHistoryInOrder() {
        List<CopilotConversationMessage> messages = List.of(
                new CopilotConversationMessage(
                        CopilotMessageRole.USER,
                        "Which vehicle is highest risk?",
                        Instant.parse("2026-07-10T10:00:00Z")
                ),
                new CopilotConversationMessage(
                        CopilotMessageRole.ASSISTANT,
                        "Ford Focus ST has the lowest score.",
                        Instant.parse("2026-07-10T10:00:01Z")
                )
        );

        String result =
                builder.buildConversationHistory(messages);

        assertThat(result)
                .containsSubsequence(
                        "USER: Which vehicle is highest risk?",
                        "ASSISTANT: Ford Focus ST has the lowest score."
                );
    }

    @Test
    void shouldBuildInitialInputWithHistoryAndQuestion() {
        String input = builder.buildInitialInput(
                "Does it have overdue maintenance?",
                List.of(
                        new CopilotConversationMessage(
                                CopilotMessageRole.ASSISTANT,
                                "Ford Focus ST has the lowest score.",
                                Instant.now()
                        )
                )
        );

        assertThat(input)
                .contains("Conversation history:")
                .contains("Ford Focus ST has the lowest score.")
                .contains("Current question:")
                .contains("Does it have overdue maintenance?");
    }

    @Test
    void instructionsShouldRequireGroundedAnswers() {
        assertThat(builder.instructions())
                .contains("Never invent fleet facts")
                .contains("tool results as the source of truth")
                .contains("Ask for clarification");
    }
}