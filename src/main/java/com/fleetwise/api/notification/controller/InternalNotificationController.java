package com.fleetwise.api.notification.controller;

import com.fleetwise.api.notification.service.DailyDigestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController {

    private final DailyDigestService dailyDigestService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/daily-digest")
    public ResponseEntity<Void> sendDailyDigest(
            @RequestHeader("X-INTERNAL-API-KEY") String apiKey
    ) {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        dailyDigestService.sendDailyDigest();

        return ResponseEntity.noContent().build();
    }
}