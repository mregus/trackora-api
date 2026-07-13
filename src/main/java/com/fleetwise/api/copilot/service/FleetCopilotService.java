package com.fleetwise.api.copilot.service;

import com.fleetwise.api.copilot.ai.FleetCopilotOpenAiClient;
import com.fleetwise.api.copilot.dto.CopilotConversationMessage;
import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.FleetCopilotResponse;
import com.fleetwise.api.copilot.dto.VehicleRiskSummary;
import com.fleetwise.api.copilot.entity.FleetCopilotConversation;
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
    private final FleetCopilotConversationService conversationService;

    public FleetCopilotResponse ask(
            UUID userId,
            UUID fleetId,
            UUID conversationId,
            String question
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        FleetCopilotConversation conversation =
                conversationService.getOrCreate(
                        conversationId,
                        fleetId,
                        userId,
                        question
                );

        conversationService.saveUserMessage(
                conversation,
                question
        );

        List<CopilotConversationMessage> previousHistory =
                conversationService.getRecentConversationHistory(
                        conversation.getId()
                );

        FleetCopilotContext context = contextService.build(fleetId);

        FleetCopilotResponse fallback =
                buildDeterministicResponse(conversation.getId(), context, question);
        FleetCopilotResponse response;

        FleetCopilotOpenAiClient client =
                openAiClientProvider.getIfAvailable();

        if (client == null) {
            response = fallback;
        } else {
            try {
                String aiAnswer = client.answerWithTools(
                        fleetId,
                        userId,
                        question,
                        buildConversationHistory(previousHistory)
                );

                response = new FleetCopilotResponse(
                        conversation.getId(),
                        aiAnswer,
                        fallback.supportingFacts(),
                        Instant.now(),
                        true
                );

            } catch (Exception ex) {
                log.warn(
                        "Fleet Copilot AI tool flow failed fleetId={}, conversationId={}, error={}",
                        fleetId,
                        conversation.getId(),
                        ex.getMessage()
                );

                response = new FleetCopilotResponse(
                        conversation.getId(),
                        fallback.answer(),
                        fallback.supportingFacts(),
                        Instant.now(),
                        false
                );
            }
        }

        conversationService.saveAssistantMessage(conversation, response);

        return response;
    }

    private FleetCopilotResponse answerSafety(UUID conversationId, FleetCopilotContext context) {
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

        return response(conversationId, answer, facts);
    }

    private FleetCopilotResponse answerDeviceHealth(
            UUID conversationId,
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

        return response(conversationId, answer, facts);
    }

    private FleetCopilotResponse answerMaintenance(
            UUID conversationId,
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

        return response(conversationId, answer, facts);
    }

    private FleetCopilotResponse answerAlerts(UUID conversationId, FleetCopilotContext context) {
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

        return response(conversationId, answer, facts);
    }

    private FleetCopilotResponse answerCosts(UUID conversationId, FleetCopilotContext context) {
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

        return response(conversationId, answer, facts);
    }

    private FleetCopilotResponse answerOverview(
            UUID conversationId,
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

        return response(conversationId, answer, facts);
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

    private FleetCopilotResponse buildDeterministicResponse(
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

    private String buildConversationHistory(
            List<CopilotConversationMessage> messages
    ) {
        if (messages.isEmpty()) {
            return "No previous conversation.";
        }

        return messages.stream()
                .map(message -> "%s: %s".formatted(
                        message.role().name(),
                        message.content()
                ))
                .collect(Collectors.joining("\n"));
    }
}