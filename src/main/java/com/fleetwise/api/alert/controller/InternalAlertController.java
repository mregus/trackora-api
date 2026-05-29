package com.fleetwise.api.alert.controller;

import com.fleetwise.api.alert.service.FuelAlertService;
import com.fleetwise.api.alert.service.MaintenanceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/alerts")
public class InternalAlertController {

    private final MaintenanceAlertService maintenanceAlertService;
    private final FuelAlertService fuelAlertService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/generate")
    public ResponseEntity<Void> generateAlerts(
            @RequestHeader("X-INTERNAL-API-KEY") String apiKey
    ) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        maintenanceAlertService.generateMaintenanceDueAlerts();
        fuelAlertService.generateFuelAnomalyAlerts();

        return ResponseEntity.noContent().build();
    }
}