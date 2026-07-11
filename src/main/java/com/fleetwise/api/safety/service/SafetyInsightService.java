package com.fleetwise.api.safety.service;

import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.safety.dto.FleetSafetyTrendPoint;
import com.fleetwise.api.safety.dto.SafetyInsightResponse;
import com.fleetwise.api.safety.dto.VehicleSafetyScoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SafetyInsightService {

    private final FleetAccessService fleetAccessService;
    private final SafetyScoreService safetyScoreService;

    @Transactional(readOnly = true)
    public SafetyInsightResponse getInsight(
            UUID userId,
            UUID fleetId
    ) {
        fleetAccessService.validateAccess(fleetId, userId);

        List<VehicleSafetyScoreResponse> scores =
                safetyScoreService.getSystemFleetSafetyScores(fleetId);

        List<FleetSafetyTrendPoint> trend =
                safetyScoreService.getSystemFleetSafetyTrend(fleetId, 30);

        return buildInsight(scores, trend);
    }

    private SafetyInsightResponse buildInsight(
            List<VehicleSafetyScoreResponse> scores,
            List<FleetSafetyTrendPoint> trend
    ) {
        if (scores.isEmpty()) {
            return new SafetyInsightResponse(
                    "There is not enough safety data to generate an analysis yet.",
                    List.of(
                            "Continue collecting telemetry before evaluating fleet safety."
                    ),
                    Instant.now()
            );
        }

        double averageScore = scores.stream()
                .mapToInt(VehicleSafetyScoreResponse::score)
                .average()
                .orElse(0);

        int hardBrakes = scores.stream()
                .mapToInt(VehicleSafetyScoreResponse::hardBrakes)
                .sum();

        int hardAccelerations = scores.stream()
                .mapToInt(VehicleSafetyScoreResponse::hardAccelerations)
                .sum();

        int harshTurns = scores.stream()
                .mapToInt(VehicleSafetyScoreResponse::harshTurns)
                .sum();

        int speedingEvents = scores.stream()
                .mapToInt(VehicleSafetyScoreResponse::speedingEvents)
                .sum();

        int idleMinutes = scores.stream()
                .mapToInt(VehicleSafetyScoreResponse::idleMinutes)
                .sum();

        VehicleSafetyScoreResponse highestRiskVehicle = scores.stream()
                .min(Comparator.comparingInt(VehicleSafetyScoreResponse::score))
                .orElseThrow();

        double scoreChange = calculateScoreChange(trend);

        String scoreLabel = safetyLabel(averageScore);

        String summary = """
                Fleet safety is currently %s with an average score of %.1f out of 100. \
                The 30-day score change is %+.1f points. The highest-risk vehicle is %s \
                with a score of %d. During the current scoring period, the fleet recorded \
                %d hard braking events, %d hard accelerations, %d harsh turns, \
                %d speeding events, and %d idle minutes.
                """.formatted(
                scoreLabel,
                averageScore,
                scoreChange,
                highestRiskVehicle.label(),
                highestRiskVehicle.score(),
                hardBrakes,
                hardAccelerations,
                harshTurns,
                speedingEvents,
                idleMinutes
        );

        List<String> recommendations = buildRecommendations(
                averageScore,
                hardBrakes,
                hardAccelerations,
                harshTurns,
                speedingEvents,
                idleMinutes,
                highestRiskVehicle
        );

        return new SafetyInsightResponse(
                summary,
                recommendations,
                Instant.now()
        );
    }

    private double calculateScoreChange(List<FleetSafetyTrendPoint> trend) {
        if (trend.size() < 2) {
            return 0;
        }

        double first = trend.get(0).averageScore();
        double last = trend.get(trend.size() - 1).averageScore();

        return Math.round((last - first) * 10.0) / 10.0;
    }

    private List<String> buildRecommendations(
            double averageScore,
            int hardBrakes,
            int hardAccelerations,
            int harshTurns,
            int speedingEvents,
            int idleMinutes,
            VehicleSafetyScoreResponse highestRiskVehicle
    ) {
        List<String> recommendations = new ArrayList<>();

        if (hardBrakes > 0) {
            recommendations.add(
                    "Coach drivers to anticipate stops earlier and maintain safer following distances."
            );
        }

        if (hardAccelerations > 0) {
            recommendations.add(
                    "Review acceleration behavior and encourage smoother throttle application."
            );
        }

        if (harshTurns > 0) {
            recommendations.add(
                    "Review harsh-turn events and reinforce safe cornering speeds."
            );
        }

        if (speedingEvents > 0) {
            recommendations.add(
                    "Review speeding events and reinforce fleet speed policies."
            );
        }

        if (idleMinutes >= 30) {
            recommendations.add(
                    "Reduce unnecessary idling to improve fuel efficiency and lower vehicle wear."
            );
        }

        if (highestRiskVehicle.checkEngine()) {
            recommendations.add(
                    "Inspect %s because it currently reports a check-engine condition."
                            .formatted(highestRiskVehicle.label())
            );
        }

        if (averageScore < 75) {
            recommendations.add(
                    "Prioritize driver coaching for the lowest-scoring vehicles."
            );
        }

        if (recommendations.isEmpty()) {
            recommendations.add(
                    "Continue monitoring current driving behavior; no major safety intervention is required."
            );
        }

        return recommendations;
    }

    private String safetyLabel(double score) {
        if (score >= 90) {
            return "excellent";
        }

        if (score >= 75) {
            return "good";
        }

        if (score >= 60) {
            return "in need of attention";
        }

        return "high risk";
    }
}