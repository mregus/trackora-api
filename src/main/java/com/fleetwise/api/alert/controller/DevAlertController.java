package com.fleetwise.api.alert.controller;

import com.fleetwise.api.alert.service.FuelAlertService;
import com.fleetwise.api.alert.service.MaintenanceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequiredArgsConstructor
public class DevAlertController {

    private final MaintenanceAlertService maintenanceAlertService;
    private final FuelAlertService fuelAlertService;

    @PostMapping("/api/dev/alerts/generate-maintenance")
    public void generateMaintenanceAlerts() {
        maintenanceAlertService.generateMaintenanceDueAlerts();
    }

    @PostMapping("/api/dev/alerts/generate-fuel")
    public void generateFuelAlerts() {
        fuelAlertService.generateFuelAnomalyAlerts();
    }
}