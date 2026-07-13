package com.fleetwise.api.ai.openai;

import com.fleetwise.api.ai.config.OpenAiProperties;
import com.fleetwise.api.common.exception.ExternalServiceException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private final OpenAiProperties properties;
    private final RestClient restClient;

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String generateText(String prompt, AiPromptType type) {

        if (!properties.enabled()) {
            return switch (type) {
                case VEHICLE -> mockVehicleSummary();
                case FLEET -> mockFleetSummary();
            };
        }

        return callOpenAi(prompt);
    }

    private String callOpenAi(String prompt) {
        try {
            Map<String, Object> request = Map.of(
                    "model", properties.model(),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are a fleet operations assistant. Provide concise, useful fleet insights."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    )
            );

            var response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            List choices = (List) response.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");

            return (String) message.get("content");

        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("AI service temporarily unavailable", e);
        }
    }

    private String mockFleetSummary() {
        return """
            Fleet AI Summary (Mock Mode):

            - Fleet operations appear stable overall.
            - Maintenance costs are within expected range for the current period.
            - Fuel usage should be reviewed for month-over-month trends.
            - Open alerts should be prioritized based on operational impact.
            - Vehicle availability appears acceptable, but in-shop units should be monitored.

            This response was generated locally because OpenAI is disabled.
            """;
    }

    private String mockVehicleSummary() {
        return """
            Vehicle AI Insight (Mock Mode):

            - Vehicle appears operationally healthy.
            - No critical maintenance risks detected.
            - Fuel consumption appears within expected range.
            - Monitor tire wear and preventive maintenance schedule.
            - Recommend reviewing upcoming service intervals.
            - No unresolved high-priority alerts found.

            This response was generated locally because OpenAI is disabled.
            """;
    }
}