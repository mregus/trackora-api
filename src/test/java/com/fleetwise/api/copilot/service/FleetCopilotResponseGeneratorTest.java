package com.fleetwise.api.copilot.service;

import com.fleetwise.api.copilot.ai.FleetCopilotOpenAiClient;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.entity.FleetCopilotConversation;
import com.fleetwise.api.copilot.observability.CopilotMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FleetCopilotResponseGeneratorTest {

    private final ObjectProvider<FleetCopilotOpenAiClient> provider =
            mock(ObjectProvider.class);

    private final CopilotMetrics metrics =
            mock(CopilotMetrics.class);

    private final FleetCopilotResponseGenerator generator =
            new FleetCopilotResponseGenerator(
                    provider,
                    metrics
            );

    @Test
    void shouldReturnFallbackWhenAiIsDisabled() {
        when(provider.getIfAvailable()).thenReturn(null);

        FleetCopilotConversation conversation =
                conversation();

        FleetCopilotResponse fallback =
                fallback(conversation.getId());

        FleetCopilotResponse response =
                generator.generate(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        conversation,
                        "What maintenance is overdue?",
                        List.of(),
                        fallback
                );

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.answer())
                .isEqualTo(fallback.answer());
        assertThat(response.conversationId())
                .isEqualTo(conversation.getId());

        verify(metrics).fallbackUsed("ai_disabled");
    }

    @Test
    void shouldReturnAiResponseWhenCallSucceeds() {
        FleetCopilotOpenAiClient client =
                mock(FleetCopilotOpenAiClient.class);

        when(provider.getIfAvailable())
                .thenReturn(client);

        when(metrics.recordAiCall(any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier =
                            invocation.getArgument(0);

                    return supplier.get();
                });

        when(client.answerWithTools(
                any(UUID.class),
                any(UUID.class),
                anyString(),
                anyList()
        )).thenReturn(
                "The fleet has three overdue maintenance items."
        );

        FleetCopilotConversation conversation =
                conversation();

        FleetCopilotResponse response =
                generator.generate(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        conversation,
                        "What maintenance is overdue?",
                        List.of(),
                        fallback(conversation.getId())
                );

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.answer())
                .contains("three overdue maintenance items");
    }

    @Test
    void shouldReturnFallbackWhenAiFails() {
        FleetCopilotOpenAiClient client =
                mock(FleetCopilotOpenAiClient.class);

        when(provider.getIfAvailable())
                .thenReturn(client);

        when(metrics.recordAiCall(any()))
                .thenThrow(
                        new IllegalStateException(
                                "OpenAI returned no final answer"
                        )
                );

        FleetCopilotConversation conversation =
                conversation();

        FleetCopilotResponse fallback =
                fallback(conversation.getId());

        FleetCopilotResponse response =
                generator.generate(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        conversation,
                        "What maintenance is overdue?",
                        List.of(),
                        fallback
                );

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.answer())
                .isEqualTo(fallback.answer());

        verify(metrics)
                .fallbackUsed("invalid_ai_response");
    }

    private FleetCopilotConversation conversation() {
        FleetCopilotConversation conversation =
                new FleetCopilotConversation();

        conversation.setId(UUID.randomUUID());

        return conversation;
    }

    private FleetCopilotResponse fallback(
            UUID conversationId
    ) {
        return new FleetCopilotResponse(
                conversationId,
                "The fleet has three overdue maintenance items.",
                List.of("Overdue maintenance items: 3"),
                Instant.now(),
                false
        );
    }
}