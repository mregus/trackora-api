package com.fleetwise.api.notification.dto;

public record NotificationSettingsResponse(
        boolean dailyDigestEnabled,
        boolean alertEmailsEnabled,
        boolean maintenanceRemindersEnabled
) {}