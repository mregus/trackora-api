package com.fleetwise.api.copilot.fallback;

import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.dto.VehicleRiskSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FleetCopilotFallbackService {

    public FleetCopilotResponse build(
            UUID conversationId,
            FleetCopilotContext context,
            String question
    ) {
        String normalized = question
                .trim()
                .toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "safety", "unsafe", "risk", "driver score")) {
            return answerSafety(conversationId, context);
        }

        if (containsAny(normalized, "offline", "online", "device", "reporting")) {
            return answerDeviceHealth(conversationId, context);
        }

        if (containsAny(normalized, "maintenance", "service", "repair", "overdue")) {
            return answerMaintenance(conversationId, context);
        }

        if (containsAny(normalized, "alert", "critical", "warning")) {
            return answerAlerts(conversationId, context);
        }

        if (containsAny(normalized, "cost", "fuel", "spend", "expense")) {
            return answerCosts(conversationId, context);
        }

        return answerOverview(conversationId, context);
    }

    private FleetCopilotResponse answerSafety(
            UUID conversationId,
            FleetCopilotContext context
    ) {
        List<String> facts = new ArrayList<>();

        facts.add(
                "Average fleet safety score: %.1f"
                        .formatted(context.averageSafetyScore())
        );

        VehicleRiskSummary highestRisk = context.highestRiskVehicles()
                .stream()
                .min(Comparator.comparingInt(VehicleRiskSummary::safetyScore))
                .orElse(null);

        if (highestRisk == null) {
            return response(
                    conversationId,
                    "There is not enough safety data to evaluate this fleet yet.",
                    facts
            );
        }

        facts.add(
                "Highest-risk vehicle: %s, score %d"
                        .formatted(
                                highestRisk.vehicleName(),
                                highestRisk.safetyScore()
                        )
        );

        facts.add(
                "Hard brakes: %d, speeding events: %d, idle minutes: %d"
                        .formatted(
                                highestRisk.hardBrakes(),
                                highestRisk.speedingEvents(),
                                highestRisk.idleMinutes()
                        )
        );

        String answer = """
                The fleet's average safety score is %.1f. The vehicle needing the
                most attention is %s with a score of %d. Review its harsh-braking,
                speeding, and idling activity before coaching the assigned driver.
                """.formatted(
                context.averageSafetyScore(),
                highestRisk.vehicleName(),
                highestRisk.safetyScore()
        );

        return response(conversationId, answer, facts);
    }

    private FleetCopilotResponse answerDeviceHealth(
            UUID conversationId,
            FleetCopilotContext context
    ) {
        return response(
                conversationId,
                """
                %d vehicles are online, %d are stale, and %d are offline.
                Prioritize offline devices first, followed by stale devices that
                have not reported recently.
                """.formatted(
                        context.onlineVehicles(),
                        context.staleVehicles(),
                        context.offlineVehicles()
                ),
                List.of(
                        "Online vehicles: " + context.onlineVehicles(),
                        "Stale vehicles: " + context.staleVehicles(),
                        "Offline vehicles: " + context.offlineVehicles()
                )
        );
    }

    private FleetCopilotResponse answerMaintenance(
            UUID conversationId,
            FleetCopilotContext context
    ) {
        return response(
                conversationId,
                """
                The fleet has %d overdue maintenance items and %d items due soon.
                Address overdue work first, especially on vehicles with critical
                alerts or low safety scores.
                """.formatted(
                        context.overdueMaintenance(),
                        context.maintenanceDueSoon()
                ),
                List.of(
                        "Overdue maintenance items: " + context.overdueMaintenance(),
                        "Maintenance due soon: " + context.maintenanceDueSoon(),
                        "Maintenance cost: $" + context.monthlyMaintenanceCost()
                )
        );
    }

    private FleetCopilotResponse answerAlerts(
            UUID conversationId,
            FleetCopilotContext context
    ) {
        return response(
                conversationId,
                """
                There are %d open alerts: %d critical and %d warnings.
                Critical alerts should be reviewed immediately, followed by recurring
                warnings affecting the same vehicle.
                """.formatted(
                        context.openAlerts(),
                        context.criticalAlerts(),
                        context.warningAlerts()
                ),
                List.of(
                        "Open alerts: " + context.openAlerts(),
                        "Critical alerts: " + context.criticalAlerts(),
                        "Warning alerts: " + context.warningAlerts()
                )
        );
    }

    private FleetCopilotResponse answerCosts(
            UUID conversationId,
            FleetCopilotContext context
    ) {
        return response(
                conversationId,
                """
                Current recorded fuel cost is $%s and maintenance cost is $%s.
                Compare these costs with vehicle mileage and idle time to identify
                inefficient vehicles.
                """.formatted(
                        context.monthlyFuelCost(),
                        context.monthlyMaintenanceCost()
                ),
                List.of(
                        "Fuel cost: $" + context.monthlyFuelCost(),
                        "Maintenance cost: $" + context.monthlyMaintenanceCost()
                )
        );
    }

    private FleetCopilotResponse answerOverview(
            UUID conversationId,
            FleetCopilotContext context
    ) {
        return response(
                conversationId,
                """
                %s has %d vehicles, with %d currently online. There are %d open
                alerts and the average safety score is %.1f. Review critical alerts,
                offline vehicles, and the lowest safety scores first.
                """.formatted(
                        context.fleetName(),
                        context.totalVehicles(),
                        context.onlineVehicles(),
                        context.openAlerts(),
                        context.averageSafetyScore()
                ),
                List.of(
                        "Total vehicles: " + context.totalVehicles(),
                        "Online vehicles: " + context.onlineVehicles(),
                        "Open alerts: " + context.openAlerts(),
                        "Average safety score: " + context.averageSafetyScore()
                )
        );
    }

    private FleetCopilotResponse response(
            UUID conversationId,
            String answer,
            List<String> facts
    ) {
        return new FleetCopilotResponse(
                conversationId,
                answer.strip(),
                facts,
                Instant.now(),
                false
        );
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }

        return false;
    }
}