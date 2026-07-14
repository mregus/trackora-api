package com.fleetwise.api.copilot.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetwise.api.copilot.observability.CopilotMetrics;
import com.fleetwise.api.copilot.tool.FleetCopilotTool;
import com.fleetwise.api.copilot.tool.FleetCopilotToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FleetCopilotOpenAiClientTest {

    private MockRestServiceServer server;
    private FleetCopilotOpenAiClient client;
    private FleetCopilotToolRegistry toolRegistry;
    private CopilotPromptBuilder copilotPromptBuilder;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules();

        toolRegistry = mock(FleetCopilotToolRegistry.class);

        FleetCopilotTool tool = mock(FleetCopilotTool.class);

        copilotPromptBuilder = mock(CopilotPromptBuilder.class);

        when(tool.name()).thenReturn("get_fleet_summary");
        when(tool.description()).thenReturn("Returns fleet summary");
        when(tool.parametersSchema()).thenReturn(
                Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of(),
                        "additionalProperties", false
                )
        );

        when(toolRegistry.all()).thenReturn(List.of(tool));

        when(toolRegistry.execute(
                eq("get_fleet_summary"),
                any(UUID.class),
                any(UUID.class),
                any()
        )).thenReturn("""
                {"totalVehicles":4}
                """);

        CopilotMetrics metrics =
                new CopilotMetrics(new SimpleMeterRegistry());

        client = new FleetCopilotOpenAiClient(
                builder,
                objectMapper,
                toolRegistry,
                metrics,
                copilotPromptBuilder,
                "https://api.openai.com/v1",
                "test-api-key",
                "test-model"
        );
    }

    @Test
    void shouldStopAfterMaximumToolRounds() {
        String repeatedFunctionCall = """
                {
                  "id": "resp_test",
                  "output": [
                    {
                      "type": "function_call",
                      "call_id": "call_test",
                      "name": "get_fleet_summary",
                      "arguments": "{}"
                    }
                  ]
                }
                """;

        /*
         * One initial response plus one response after each of the five
         * permitted tool rounds.
         */
        server.expect(
                        ExpectedCount.times(6),
                        requestTo("https://api.openai.com/v1/responses")
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                repeatedFunctionCall,
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThatThrownBy(() ->
                client.answerWithTools(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Keep calling tools",
                        List.of()
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "exceeded maximum tool rounds"
                );

        server.verify();

        verify(toolRegistry, times(5)).execute(
                eq("get_fleet_summary"),
                any(UUID.class),
                any(UUID.class),
                any()
        );
    }
}