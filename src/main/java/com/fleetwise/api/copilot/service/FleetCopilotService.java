package com.fleetwise.api.copilot.service;

import com.fleetwise.api.copilot.ai.FleetCopilotOpenAiClient;
import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.dto.VehicleRiskSummary;
import com.fleetwise.api.fleet.service.FleetAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FleetCopilotService {

    private final FleetAccessService fleetAccessService;
    private final FleetCopilotContextService contextService;
    private final ObjectProvider<FleetCopilotOpenAiClient> openAiClientProvider;

    public FleetCopilotResponse ask(
            UUID userId,
            UUID fleetId,
            String question
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        FleetCopilotContext context = contextService.build(fleetId);

        FleetCopilotResponse fallback =
                buildDeterministicResponse(context, question);

        FleetCopilotOpenAiClient client =
                openAiClientProvider.getIfAvailable();

        if (client == null) {
            return fallback;
        }

        try {
            String aiAnswer = client.rewrite(
                    question,
                    fallback.answer(),
                    buildAiContext(context)
            );

            return new FleetCopilotResponse(
                    aiAnswer,
                    fallback.supportingFacts(),
                    Instant.now(),
                    true
            );

        } catch (Exception ex) {
            log.warn(
                    "Fleet Copilot AI rewrite failed fleetId={}, error={}",
                    fleetId,
                    ex.getMessage()
            );

            return fallback;
        }
    }

    private FleetCopilotResponse answerSafety(FleetCopilotContext context) {
        List<String> facts = new ArrayList<>();

        facts.add("Average fleet safety score: %.1f"
                .formatted(context.averageSafetyScore()));

        VehicleRiskSummary highestRisk = context.highestRiskVehicles()
                .stream()
                .min(Comparator.comparingInt(VehicleRiskSummary::safetyScore))
                .orElse(null);

        String answer;

        if (highestRisk == null) {
            answer = "There is not enough safety data to evaluate this fleet yet.";
        } else {
            facts.add("Highest-risk vehicle: %s, score %d"
                    .formatted(
                            highestRisk.vehicleName(),
                            highestRisk.safetyScore()
                    ));

            facts.add("Hard brakes: %d, speeding events: %d, idle minutes: %d"
                    .formatted(
                            highestRisk.hardBrakes(),
                            highestRisk.speedingEvents(),
                            highestRisk.idleMinutes()
                    ));

            answer = """
                    The fleet's average safety score is %.1f. The vehicle needing the
                    most attention is %s with a score of %d. Review its harsh-braking,
                    speeding, and idling activity before coaching the assigned driver.
                    """.formatted(
                    context.averageSafetyScore(),
                    highestRisk.vehicleName(),
                    highestRisk.safetyScore()
            );
        }

        return response(answer, facts);
    }

    private FleetCopilotResponse answerDeviceHealth(
            FleetCopilotContext context
    ) {
        List<String> facts = List.of(
                "Online vehicles: " + context.onlineVehicles(),
                "Stale vehicles: " + context.staleVehicles(),
                "Offline vehicles: " + context.offlineVehicles()
        );

        String answer = """
                %d vehicles are online, %d are stale, and %d are offline.
                Prioritize offline devices first, followed by stale devices that
                have not reported recently.
                """.formatted(
                context.onlineVehicles(),
                context.staleVehicles(),
                context.offlineVehicles()
        );

        return response(answer, facts);
    }

    private FleetCopilotResponse answerMaintenance(
            FleetCopilotContext context
    ) {
        List<String> facts = List.of(
                "Overdue maintenance items: " + context.overdueMaintenance(),
                "Maintenance due soon: " + context.maintenanceDueSoon(),
                "Maintenance cost: $" + context.monthlyMaintenanceCost()
        );

        String answer = """
                The fleet has %d overdue maintenance items and %d items due soon.
                Address overdue work first, especially on vehicles with critical
                alerts or low safety scores.
                """.formatted(
                context.overdueMaintenance(),
                context.maintenanceDueSoon()
        );

        return response(answer, facts);
    }

    private FleetCopilotResponse answerAlerts(FleetCopilotContext context) {
        List<String> facts = List.of(
                "Open alerts: " + context.openAlerts(),
                "Critical alerts: " + context.criticalAlerts(),
                "Warning alerts: " + context.warningAlerts()
        );

        String answer = """
                There are %d open alerts: %d critical and %d warnings.
                Critical alerts should be reviewed immediately, followed by recurring
                warnings affecting the same vehicle.
                """.formatted(
                context.openAlerts(),
                context.criticalAlerts(),
                context.warningAlerts()
        );

        return response(answer, facts);
    }

    private FleetCopilotResponse answerCosts(FleetCopilotContext context) {
        List<String> facts = List.of(
                "Fuel cost: $" + context.monthlyFuelCost(),
                "Maintenance cost: $" + context.monthlyMaintenanceCost()
        );

        String answer = """
                Current recorded fuel cost is $%s and maintenance cost is $%s.
                Compare these costs with vehicle mileage and idle time to identify
                inefficient vehicles.
                """.formatted(
                context.monthlyFuelCost(),
                context.monthlyMaintenanceCost()
        );

        return response(answer, facts);
    }

    private FleetCopilotResponse answerOverview(
            FleetCopilotContext context
    ) {
        List<String> facts = List.of(
                "Total vehicles: " + context.totalVehicles(),
                "Online vehicles: " + context.onlineVehicles(),
                "Open alerts: " + context.openAlerts(),
                "Average safety score: " + context.averageSafetyScore()
        );

        String answer = """
                %s has %d vehicles, with %d currently online. There are %d open
                alerts and the average safety score is %.1f. Review critical alerts,
                offline vehicles, and the lowest safety scores first.
                """.formatted(
                context.fleetName(),
                context.totalVehicles(),
                context.onlineVehicles(),
                context.openAlerts(),
                context.averageSafetyScore()
        );

        return response(answer, facts);
    }

    private FleetCopilotResponse response(
            String answer,
            List<String> facts
    ) {
        return new FleetCopilotResponse(
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

    private FleetCopilotResponse buildDeterministicResponse(
            FleetCopilotContext context,
            String question
    ) {
        String normalized = question
                .trim()
                .toLowerCase(Locale.ROOT);

        if (containsAny(normalized, "safety", "unsafe", "risk", "driver score")) {
            return answerSafety(context);
        }

        if (containsAny(normalized, "offline", "online", "device", "reporting")) {
            return answerDeviceHealth(context);
        }

        if (containsAny(normalized, "maintenance", "service", "repair", "overdue")) {
            return answerMaintenance(context);
        }

        if (containsAny(normalized, "alert", "critical", "warning")) {
            return answerAlerts(context);
        }

        if (containsAny(normalized, "cost", "fuel", "spend", "expense")) {
            return answerCosts(context);
        }

        return answerOverview(context);
    }

    private String buildAiContext(FleetCopilotContext context) {
        String riskVehicles = context.highestRiskVehicles()
                .stream()
                .map(vehicle -> """
                    - %s, plate %s, safety score %d, hard brakes %d, \
                    speeding events %d, idle minutes %d, check engine %s
                    """.formatted(
                        vehicle.vehicleName(),
                        vehicle.licensePlate(),
                        vehicle.safetyScore(),
                        vehicle.hardBrakes(),
                        vehicle.speedingEvents(),
                        vehicle.idleMinutes(),
                        vehicle.checkEngine()
                ))
                .collect(Collectors.joining("\n"));

        return """
            Fleet: %s
            Total vehicles: %d
            Active vehicles: %d
            Online vehicles: %d
            Stale vehicles: %d
            Offline vehicles: %d

            Open alerts: %d
            Critical alerts: %d
            Warning alerts: %d

            Overdue maintenance: %d
            Maintenance due soon: %d

            Monthly maintenance cost: %s
            Monthly fuel cost: %s
            Average safety score: %.1f

            Highest-risk vehicles:
            %s
            """.formatted(
                context.fleetName(),
                context.totalVehicles(),
                context.activeVehicles(),
                context.onlineVehicles(),
                context.staleVehicles(),
                context.offlineVehicles(),
                context.openAlerts(),
                context.criticalAlerts(),
                context.warningAlerts(),
                context.overdueMaintenance(),
                context.maintenanceDueSoon(),
                context.monthlyMaintenanceCost(),
                context.monthlyFuelCost(),
                context.averageSafetyScore(),
                riskVehicles.isBlank() ? "No safety score data available." : riskVehicles
        );
    }
}