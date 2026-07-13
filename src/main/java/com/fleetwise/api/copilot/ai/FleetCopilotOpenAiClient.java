package com.fleetwise.api.copilot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.ai.dto.OpenAiToolRequest;
import com.fleetwise.api.ai.dto.OpenAiToolResponse;
import com.fleetwise.api.copilot.tool.FleetCopilotToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "openai.enabled",
        havingValue = "true"
)
public class FleetCopilotOpenAiClient {

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;
    private final FleetCopilotToolRegistry toolRegistry;

    public FleetCopilotOpenAiClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            FleetCopilotToolRegistry toolRegistry,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.model}") String model
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.model = model;
    }

    public String answerWithTools(
            UUID fleetId,
            UUID userId,
            String question,
            String conversationHistory
    ) {
        OpenAiToolResponse first = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OpenAiToolRequest(
                        model,
                        copilotInstructions(),
                        """
                        Conversation history:
                        %s

                        Current question:
                        %s
                        """.formatted(
                                conversationHistory,
                                question
                        ),
                        buildTools(),
                        "auto"
                ))
                .retrieve()
                .body(OpenAiToolResponse.class);

        if (first == null) {
            throw new IllegalStateException(
                    "OpenAI returned no response"
            );
        }

        log.info(
                "Copilot first OpenAI response id={}, functionCalls={}",
                first.id(),
                first.functionCalls().size()
        );

        List<OpenAiToolResponse.OutputItem> functionCalls =
                first.functionCalls();

        if (functionCalls.isEmpty()) {
            String text = first.outputText();

            if (text == null || text.isBlank()) {
                throw new IllegalStateException(
                        "OpenAI returned neither text nor tool calls"
                );
            }

            return text.strip();
        }

        List<Map<String, Object>> toolOutputs =
                executeToolCalls(
                        first,
                        fleetId,
                        userId
                );

        OpenAiToolResponse second = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "previous_response_id", first.id(),
                        "input", toolOutputs
                ))
                .retrieve()
                .body(OpenAiToolResponse.class);

        if (second == null) {
            throw new IllegalStateException(
                    "OpenAI returned no response after tool execution"
            );
        }

        log.info(
                "Copilot tool flow completed responseId={}",
                second.id()
        );

        String finalText = second.outputText();

        if (finalText == null || finalText.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI returned no final answer after tool execution"
            );
        }

        return finalText.strip();
    }

    private List<Map<String, Object>> executeToolCalls(
            OpenAiToolResponse response,
            UUID fleetId,
            UUID userId
    ) {
        return response.functionCalls()
                .stream()
                .map(call -> executeToolCall(
                        call,
                        fleetId,
                        userId
                ))
                .toList();
    }

    private Map<String, Object> executeToolCall(
            OpenAiToolResponse.OutputItem call,
            UUID fleetId,
            UUID userId
    ) {
        try {
            JsonNode arguments = parseArguments(
                    call.arguments()
            );

            String output = toolRegistry.execute(
                    call.name(),
                    fleetId,
                    userId,
                    arguments
            );

            log.info(
                    "Executing Copilot tool name={}, callId={}, fleetId={}",
                    call.name(),
                    call.callId(),
                    fleetId
            );

            return Map.of(
                    "type", "function_call_output",
                    "call_id", call.callId(),
                    "output", output
            );

        } catch (Exception ex) {
            log.error(
                    "Copilot tool failed tool={}, callId={}, fleetId={}",
                    call.name(),
                    call.callId(),
                    fleetId,
                    ex
            );

            throw new IllegalStateException(
                    "Copilot tool failed: " + call.name(),
                    ex
            );
        }
    }

    private JsonNode parseArguments(String arguments) {
        try {
            if (arguments == null || arguments.isBlank()) {
                return objectMapper.createObjectNode();
            }

            return objectMapper.readTree(arguments);

        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Invalid tool arguments",
                    ex
            );
        }
    }

    private List<OpenAiFunctionTool> buildTools() {
        return toolRegistry.all()
                .stream()
                .map(tool -> new OpenAiFunctionTool(
                        "function",
                        tool.name(),
                        tool.description(),
                        tool.parametersSchema(),
                        true
                ))
                .toList();
    }

    private String copilotInstructions() {
        return """
                You are Trackora Fleet Copilot.

                Use the available tools whenever current fleet facts are required.

                Rules:
                - Never invent vehicles, costs, alerts, maintenance records, or metrics.
                - Treat tool results as the source of truth.
                - Use conversation history only to interpret references and follow-up questions.
                - Do not expose internal implementation details.
                - Clearly state when available data is insufficient.
                - Keep answers concise, practical, and operational.
                """;
    }
}