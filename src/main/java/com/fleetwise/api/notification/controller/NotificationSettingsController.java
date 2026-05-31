package com.fleetwise.api.notification.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.notification.dto.NotificationSettingsResponse;
import com.fleetwise.api.notification.dto.UpdateNotificationSettingsRequest;
import com.fleetwise.api.notification.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    @GetMapping
    public NotificationSettingsResponse getSettings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return notificationSettingsService.getSettings(principal.getId());
    }

    @PutMapping
    public NotificationSettingsResponse updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateNotificationSettingsRequest request
    ) {
        return notificationSettingsService.updateSettings(
                principal.getId(),
                request
        );
    }
}