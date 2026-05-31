package com.fleetwise.api.notification.dto;

public record UpdateNotificationSettingsRequest(
        boolean dailyDigestEnabled,
        boolean alertEmailsEnabled,
        boolean maintenanceRemindersEnabled
) {}