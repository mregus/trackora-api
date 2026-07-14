package com.fleetwise.api.copilot.fallback;

import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.dto.VehicleRiskSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FleetCopilotFallbackServiceTest {

    private final FleetCopilotFallbackService service =
            new FleetCopilotFallbackService();

    @Test
    void shouldBuildSafetyResponse() {
        UUID conversationId = UUID.randomUUID();

        FleetCopilotContext context = context();

        FleetCopilotResponse response = service.build(
                conversationId,
                context,
                "Which vehicle has the highest safety risk?"
        );

        assertThat(response.conversationId())
                .isEqualTo(conversationId);

        assertThat(response.aiGenerated())
                .isFalse();

        assertThat(response.answer())
                .contains("Ford Focus ST")
                .contains("72");

        assertThat(response.supportingFacts())
                .contains(
                        "Average fleet safety score: 81.5",
                        "Highest-risk vehicle: Ford Focus ST, score 72"
                );
    }

    @Test
    void shouldBuildMaintenanceResponse() {
        FleetCopilotResponse response = service.build(
                UUID.randomUUID(),
                context(),
                "What maintenance should I prioritize?"
        );

        assertThat(response.answer())
                .contains("3 overdue maintenance items")
                .contains("1 items due soon");

        assertThat(response.aiGenerated())
                .isFalse();
    }

    @Test
    void shouldDefaultToOverview() {
        FleetCopilotResponse response = service.build(
                UUID.randomUUID(),
                context(),
                "Tell me what is happening"
        );

        assertThat(response.answer())
                .contains("Demo Fleet")
                .contains("4 vehicles");
    }

    private FleetCopilotContext context() {
        return new FleetCopilotContext(
                UUID.randomUUID(),
                "Demo Fleet",
                4,
                3,
                1,
                1,
                2,
                12,
                3,
                9,
                3,
                1,
                BigDecimal.valueOf(1320.99),
                BigDecimal.valueOf(244.90),
                81.5,
                List.of(
                        new VehicleRiskSummary(
                                UUID.randomUUID(),
                                "Ford Focus ST",
                                "DEMO-003",
                                72,
                                4,
                                2,
                                35,
                                true
                        )
                )
        );
    }
}