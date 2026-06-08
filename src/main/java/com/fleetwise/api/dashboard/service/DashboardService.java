package com.fleetwise.api.dashboard.service;

import com.fleetwise.api.ai.entity.AiInsight;
import com.fleetwise.api.ai.repository.AiInsightRepository;
import com.fleetwise.api.alert.entity.Alert;
import com.fleetwise.api.alert.entity.AlertSeverity;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.dashboard.dto.FleetHealthBreakdown;
import com.fleetwise.api.dashboard.dto.FleetRecommendationResponse;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.fleet.service.FleetAccessService;
import com.fleetwise.api.fuel.repository.FuelLogRepository;
import com.fleetwise.api.maintenance.entity.Maintenance;
import com.fleetwise.api.maintenance.entity.MaintenanceStatus;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.vehicle.entity.VehicleStatus;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final FuelLogRepository fuelLogRepository;
    private final AiInsightRepository aiInsightRepository;
    private final FleetAccessService fleetAccessService;

    @Transactional(readOnly = true)
    public List<FleetRecommendationResponse> getRecommendations(
            UUID fleetId,
            UUID ownerId
    ) {
        fleetAccessService.validateAccess(fleetId, ownerId);

        List<FleetRecommendationResponse> recommendations = new ArrayList<>();

        addCriticalAlertRecommendations(fleetId, recommendations);
        addMaintenanceRecommendations(fleetId, recommendations);
        addFleetHealthRecommendation(fleetId, recommendations);

        return recommendations.stream()
                .limit(5)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UUID fleetId, UUID ownerId) {
        Fleet fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        fleetAccessService.validateAccess(fleetId, ownerId);

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        long totalVehicles = vehicleRepository.countByFleetId(fleetId);
        long activeVehicles = vehicleRepository.countByFleetIdAndStatus(fleetId, VehicleStatus.ACTIVE);
        long vehiclesInShop = vehicleRepository.countByFleetIdAndStatus(fleetId, VehicleStatus.IN_SHOP);
        long outOfServiceVehicles = vehicleRepository.countByFleetIdAndStatus(fleetId, VehicleStatus.OUT_OF_SERVICE);

        long openAlerts = alertRepository.countByFleetIdAndResolvedFalse(fleetId);

//        BigDecimal monthlyMaintenanceCost =
//                maintenanceRepository.sumCostByFleetIdSince(fleetId, startOfMonth);
//
//        BigDecimal monthlyFuelCost =
//                fuelLogRepository.sumCostByFleetIdSince(fleetId, startOfMonth);

        BigDecimal monthlyMaintenanceCost =
                maintenanceRepository.sumMaintenanceCostByFleetId(fleetId);

        BigDecimal monthlyFuelCost =
                fuelLogRepository.sumFuelCostByFleetId(fleetId);

        String latestAiInsight = aiInsightRepository.findFirstByFleetIdAndVehicleIsNullOrderByCreatedAtDesc(fleetId)
                .map(AiInsight::getSummary)
                .orElse(null);

        FleetHealthBreakdown healthBreakdown =
                calculateFleetHealthBreakdown(fleetId);

        int fleetHealthScore =
                calculateFleetHealthScore(healthBreakdown);

        return new DashboardSummaryResponse(
                fleet.getId(),
                fleet.getName(),
                totalVehicles,
                activeVehicles,
                vehiclesInShop,
                outOfServiceVehicles,
                openAlerts,
                monthlyMaintenanceCost,
                monthlyFuelCost,
                latestAiInsight,
                fleetHealthScore,
                healthBreakdown
        );
    }

    private int calculateFleetHealthScore(FleetHealthBreakdown breakdown) {
        int score = 100;

        score -= breakdown.criticalAlerts() * 10;
        score -= breakdown.warningAlerts() * 5;
        score -= breakdown.overdueMaintenance() * 5;
        score -= breakdown.maintenanceDueSoon() * 2;

        return Math.max(score, 0);
    }

    private FleetHealthBreakdown calculateFleetHealthBreakdown(UUID fleetId) {
        List<Alert> alerts = alertRepository.findByFleetId(fleetId)
                .stream()
                .filter(alert -> !alert.isResolved())
                .toList();

        int criticalAlerts = (int) alerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        int warningAlerts = (int) alerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        List<Maintenance> maintenance =
                maintenanceRepository.findByVehicleFleetId(fleetId);

        LocalDate today = LocalDate.now();

        int overdueMaintenance = 0;
        int maintenanceDueSoon = 0;

        for (Maintenance record : maintenance) {
            if (record.getStatus() == MaintenanceStatus.COMPLETED) {
                continue;
            }

            if (record.getServiceDate() == null) {
                continue;
            }

            if (record.getServiceDate().isBefore(today)) {
                overdueMaintenance++;
            } else if (!record.getServiceDate().isAfter(today.plusDays(7))) {
                maintenanceDueSoon++;
            }
        }

        return new FleetHealthBreakdown(
                criticalAlerts,
                warningAlerts,
                overdueMaintenance,
                maintenanceDueSoon
        );
    }

    private void addCriticalAlertRecommendations(
            UUID fleetId,
            List<FleetRecommendationResponse> recommendations
    ) {
        List<Alert> openAlerts = alertRepository.findByFleetId(fleetId)
                .stream()
                .filter(alert -> !alert.isResolved())
                .toList();

        long criticalCount = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        if (criticalCount > 0) {
            recommendations.add(new FleetRecommendationResponse(
                    "ALERT",
                    "%d critical alert%s require attention.".formatted(
                            criticalCount,
                            criticalCount == 1 ? "" : "s"
                    ),
                    "CRITICAL"
            ));
        }

        long warningCount = openAlerts.stream()
                .filter(alert -> alert.getSeverity() == AlertSeverity.WARNING)
                .count();

        if (warningCount >= 3) {
            recommendations.add(new FleetRecommendationResponse(
                    "ALERT",
                    "%d warning alerts are currently open.".formatted(warningCount),
                    "WARNING"
            ));
        }
    }

    private void addMaintenanceRecommendations(
            UUID fleetId,
            List<FleetRecommendationResponse> recommendations
    ) {
        List<Maintenance> records =
                maintenanceRepository.findByVehicleFleetId(fleetId);

        LocalDate today = LocalDate.now();

        long overdue = records.stream()
                .filter(record -> record.getStatus() != MaintenanceStatus.COMPLETED)
                .filter(record -> record.getServiceDate() != null)
                .filter(record -> record.getServiceDate().isBefore(today))
                .count();

        if (overdue > 0) {
            recommendations.add(new FleetRecommendationResponse(
                    "MAINTENANCE",
                    "%d maintenance item%s overdue.".formatted(
                            overdue,
                            overdue == 1 ? " is" : "s are"
                    ),
                    "CRITICAL"
            ));
        }

        long dueSoon = records.stream()
                .filter(record -> record.getStatus() != MaintenanceStatus.COMPLETED)
                .filter(record -> record.getServiceDate() != null)
                .filter(record ->
                        !record.getServiceDate().isBefore(today)
                                && !record.getServiceDate().isAfter(today.plusDays(7))
                )
                .count();

        if (dueSoon > 0) {
            recommendations.add(new FleetRecommendationResponse(
                    "MAINTENANCE",
                    "%d maintenance item%s due within 7 days.".formatted(
                            dueSoon,
                            dueSoon == 1 ? " is" : "s are"
                    ),
                    "WARNING"
            ));
        }
    }

    private void addFleetHealthRecommendation(
            UUID fleetId,
            List<FleetRecommendationResponse> recommendations
    ) {
        FleetHealthBreakdown breakdown =
                calculateFleetHealthBreakdown(fleetId);

        int score = calculateFleetHealthScore(breakdown);

        if (score < 50) {
            recommendations.add(new FleetRecommendationResponse(
                    "HEALTH",
                    "Fleet health is critical. Prioritize alerts and overdue maintenance.",
                    "CRITICAL"
            ));
        } else if (score < 70) {
            recommendations.add(new FleetRecommendationResponse(
                    "HEALTH",
                    "Fleet health needs attention. Review warnings and upcoming service.",
                    "WARNING"
            ));
        }
    }
}