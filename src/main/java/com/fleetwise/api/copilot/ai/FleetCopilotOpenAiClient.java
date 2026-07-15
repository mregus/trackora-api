package com.fleetwise.api.copilot.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.ai.dto.OpenAiToolRequest;
import com.fleetwise.api.ai.dto.OpenAiToolResponse;
import com.fleetwise.api.copilot.ai.dto.OpenAiFunctionTool;
import com.fleetwise.api.copilot.conversation.dto.CopilotConversationMessage;
import com.fleetwise.api.copilot.observability.CopilotMetrics;
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

    private static final int MAX_TOOL_ROUNDS = 5;
    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;
    private final FleetCopilotToolRegistry toolRegistry;
    private final CopilotMetrics copilotMetrics;
    private final CopilotPromptBuilder copilotPromptBuilder;

    public FleetCopilotOpenAiClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            FleetCopilotToolRegistry toolRegistry,
            CopilotMetrics copilotMetrics,
            CopilotPromptBuilder copilotPromptBuilder,
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
        this.copilotMetrics = copilotMetrics;
        this.copilotPromptBuilder = copilotPromptBuilder;
        this.model = model;
    }

    public String answerWithTools(
            UUID fleetId,
            UUID userId,
            String question,
            List<CopilotConversationMessage> history
    ) {
        OpenAiToolResponse response = sendInitialRequest(
                question,
                history
        );

        for (int round = 1; round <= MAX_TOOL_ROUNDS; round++) {
            List<OpenAiToolResponse.OutputItem> calls =
                    response.functionCalls();

            if (calls.isEmpty()) {
                String text = response.outputText();

                if (text == null || text.isBlank()) {
                    throw new IllegalStateException(
                            "OpenAI returned neither text nor tool calls"
                    );
                }

                log.info(
                        "Copilot completed fleetId={}, rounds={}",
                        fleetId,
                        round
                );

                copilotMetrics.toolRound(calls.size());

                return text.strip();
            }

            log.info(
                    "Copilot tool round fleetId={}, round={}, calls={}",
                    fleetId,
                    round,
                    calls.stream()
                            .map(OpenAiToolResponse.OutputItem::name)
                            .toList()
            );

            List<Map<String, Object>> outputs =
                    executeToolCalls(response, fleetId, userId);

            response = sendToolOutputs(
                    response.id(),
                    outputs
            );
        }

        throw new IllegalStateException(
                "Copilot exceeded maximum tool rounds: "
                        + MAX_TOOL_ROUNDS
        );
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

            String output = copilotMetrics.recordToolCall(
                    call.name(),
                    () -> toolRegistry.execute(
                            call.name(),
                            fleetId,
                            userId,
                            arguments
                    )
            );

            copilotMetrics.toolSucceeded(call.name());

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

            copilotMetrics.toolFailed(
                    call.name(),
                    classifyToolFailure(ex)
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

            Tool-use rules:
            - You may call multiple tools when the question requires comparison
              across safety, maintenance, alerts, costs, vehicles, or trips.
            - Do not answer factual fleet questions from memory.
            - Use tool results as the source of truth.
            - When a vehicle reference is ambiguous, ask the user to clarify.
            - Do not invent vehicle names, costs, alerts, trips, or maintenance data.
            - Clearly distinguish current recorded data from estimates.
            - If a tool returns no records, say that no matching records were found.
            - Keep the final response concise and operational.
            - Do not mention tool names, prompts, JSON, or implementation details.
            """;
    }

    private String classifyToolFailure(Exception ex) {
        if (ex instanceof IllegalArgumentException) {
            return "invalid_arguments";
        }

        String name = ex.getClass().getSimpleName();

        if (name.contains("ResourceNotFound")) {
            return "not_found";
        }

        if (name.contains("DataAccess")) {
            return "database_error";
        }

        return "unknown";
    }

    private OpenAiToolResponse sendInitialRequest(
            String question,
            List<CopilotConversationMessage> history
    ) {
        OpenAiToolResponse response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OpenAiToolRequest(
                        model,
                        copilotPromptBuilder.instructions(),
                        copilotPromptBuilder.buildInitialInput(
                                question,
                                history
                        ),
                        buildTools(),
                        "auto"
                ))
                .retrieve()
                .body(OpenAiToolResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "OpenAI returned no initial response"
            );
        }

        return response;
    }

    private OpenAiToolResponse sendToolOutputs(
            String previousResponseId,
            List<Map<String, Object>> outputs
    ) {
        OpenAiToolResponse response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "previous_response_id", previousResponseId,
                        "input", outputs
                ))
                .retrieve()
                .body(OpenAiToolResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "OpenAI returned no response after tool execution"
            );
        }

        return response;
    }
}