package com.fleetwise.api.copilot.service;

import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.copilot.dto.FleetCopilotContext;
import com.fleetwise.api.copilot.dto.VehicleRiskSummary;
import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.service.DashboardService;
import com.fleetwise.api.safety.repository.VehicleSafetyScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetCopilotContextService {

    private final DashboardService dashboardService;
    private final AlertRepository alertRepository;
    private final VehicleSafetyScoreRepository safetyScoreRepository;

    @Transactional(readOnly = true)
    public FleetCopilotContext build(UUID fleetId) {
        DashboardSummaryResponse dashboard =
                dashboardService.getSystemSummary(fleetId);

        long criticalAlerts = alertRepository.findByFleetId(fleetId)
                .stream()
                .filter(alert -> !alert.isResolved())
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        long warningAlerts = alertRepository.findByFleetId(fleetId)
                .stream()
                .filter(alert -> !alert.isResolved())
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        var safetyScores = safetyScoreRepository.findScoresByFleetId(fleetId);

        double averageSafetyScore = safetyScores.stream()
                .mapToInt(score -> score.getScore())
                .average()
                .orElse(0);

        var highestRiskVehicles = safetyScores.stream()
                .sorted((left, right) ->
                        Integer.compare(left.getScore(), right.getScore()))
                .limit(5)
                .map(score -> new VehicleRiskSummary(
                        score.getVehicle().getId(),
                        "%s %s".formatted(
                                score.getVehicle().getMake(),
                                score.getVehicle().getModel()
                        ),
                        score.getVehicle().getLicensePlate(),
                        score.getScore(),
                        score.getHardBrakes(),
                        score.getSpeedingEvents(),
                        score.getIdleMinutes(),
                        score.isCheckEngine()
                ))
                .toList();

        return new FleetCopilotContext(
                dashboard.fleetId(),
                dashboard.fleetName(),

                dashboard.totalVehicles(),
                dashboard.activeVehicles(),
                dashboard.onlineVehicles(),
                dashboard.staleVehicles(),
                dashboard.offlineVehicles(),

                dashboard.openAlerts(),
                criticalAlerts,
                warningAlerts,

                dashboard.fleetHealthBreakdown().overdueMaintenance(),
                dashboard.fleetHealthBreakdown().maintenanceDueSoon(),

                dashboard.monthlyMaintenanceCost(),
                dashboard.monthlyFuelCost(),

                Math.round(averageSafetyScore * 10.0) / 10.0,
                highestRiskVehicles
        );
    }
}