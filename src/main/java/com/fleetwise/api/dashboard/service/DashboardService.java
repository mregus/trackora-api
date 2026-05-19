package com.fleetwise.api.dashboard.service;

import com.fleetwise.api.ai.repository.AiInsightRepository;
import com.fleetwise.api.alert.repository.AlertRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.dashboard.dto.DashboardSummaryResponse;
import com.fleetwise.api.fleet.entity.Fleet;
import com.fleetwise.api.fleet.repository.FleetRepository;
import com.fleetwise.api.fuel.repository.FuelLogRepository;
import com.fleetwise.api.maintenance.repository.MaintenanceRepository;
import com.fleetwise.api.vehicle.entity.VehicleStatus;
import com.fleetwise.api.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UUID fleetId, UUID ownerId) {
        Fleet fleet = fleetRepository.findByIdAndOwnerId(fleetId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet not found"));

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);

        long totalVehicles = vehicleRepository.countByFleetId(fleetId);
        long activeVehicles = vehicleRepository.countByFleetIdAndStatus(fleetId, VehicleStatus.ACTIVE);
        long vehiclesInShop = vehicleRepository.countByFleetIdAndStatus(fleetId, VehicleStatus.IN_SHOP);
        long outOfServiceVehicles = vehicleRepository.countByFleetIdAndStatus(fleetId, VehicleStatus.OUT_OF_SERVICE);

        long openAlerts = alertRepository.countByFleetIdAndResolvedFalse(fleetId);

        BigDecimal monthlyMaintenanceCost =
                maintenanceRepository.sumCostByFleetIdSince(fleetId, startOfMonth);

        BigDecimal monthlyFuelCost =
                fuelLogRepository.sumCostByFleetIdSince(fleetId, startOfMonth);

        String latestAiInsight = aiInsightRepository.findFirstByFleetIdAndVehicleIsNullOrderByCreatedAtDesc(fleetId)
                .map(i -> i.getSummary())
                .orElse(null);

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
                latestAiInsight
        );
    }
}