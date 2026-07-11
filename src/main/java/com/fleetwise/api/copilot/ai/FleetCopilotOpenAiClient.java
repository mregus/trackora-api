package com.fleetwise.api.copilot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "openai.enabled",
        havingValue = "true"
)
public class FleetCopilotOpenAiClient {

    private final RestClient restClient;
    private final String model;

    public FleetCopilotOpenAiClient(
            RestClient.Builder builder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        this.model = model;
    }

    public String rewrite(
            String question,
            String deterministicAnswer,
            String fleetContext
    ) {
        OpenAiResponsesRequest request = new OpenAiResponsesRequest(
                model,
                """
                You are Trackora Fleet Copilot.

                Answer using only the fleet facts provided by the application.

                Rules:
                - Do not invent vehicles, costs, alerts, dates, drivers, or metrics.
                - Do not claim access to live data beyond the supplied context.
                - Keep the response concise and operational.
                - Clearly state when there is insufficient information.
                - Recommend practical next actions when appropriate.
                - Do not mention prompts, JSON, internal services, or implementation details.
                """,
                """
                User question:
                %s

                Verified fleet context:
                %s

                Deterministic fallback analysis:
                %s

                Rewrite the answer conversationally while preserving all factual values.
                """.formatted(
                        question,
                        fleetContext,
                        deterministicAnswer
                )
        );

        OpenAiResponsesResponse response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiResponsesResponse.class);

        if (response == null) {
            throw new IllegalStateException("OpenAI returned an empty response");
        }

        String text = response.outputText();

        if (text == null || text.isBlank()) {
            throw new IllegalStateException("OpenAI response contained no output text");
        }

        return text.strip();
    }
}