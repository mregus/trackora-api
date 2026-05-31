package com.fleetwise.api.notification.controller;

import com.fleetwise.api.auth.security.UserPrincipal;
import com.fleetwise.api.notification.dto.NotificationResponse;
import com.fleetwise.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return notificationService.getNotifications(principal.getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return Map.of("count", notificationService.getUnreadCount(principal.getId()));
    }

    @PutMapping("/{notificationId}/read")
    public void markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID notificationId
    ) {
        notificationService.markAsRead(principal.getId(), notificationId);
    }

    @PutMapping("/read-all")
    public void markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal.getId());
    }
}