package com.fleetwise.api.notification.service;

import com.fleetwise.api.auth.repository.UserRepository;
import com.fleetwise.api.common.exception.ResourceNotFoundException;
import com.fleetwise.api.notification.dto.NotificationSettingsResponse;
import com.fleetwise.api.notification.dto.UpdateNotificationSettingsRequest;
import com.fleetwise.api.notification.entity.UserNotificationSettings;
import com.fleetwise.api.notification.repository.UserNotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final UserNotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(UUID userId) {
        UserNotificationSettings settings = getOrCreate(userId);
        return toResponse(settings);
    }

    @Transactional
    public NotificationSettingsResponse updateSettings(
            UUID userId,
            UpdateNotificationSettingsRequest request
    ) {
        UserNotificationSettings settings = getOrCreate(userId);

        settings.setDailyDigestEnabled(request.dailyDigestEnabled());
        settings.setAlertEmailsEnabled(request.alertEmailsEnabled());
        settings.setMaintenanceRemindersEnabled(request.maintenanceRemindersEnabled());

        return toResponse(settings);
    }

    private UserNotificationSettings getOrCreate(UUID userId) {
        return settingsRepository.findById(userId)
                .orElseGet(() -> {
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                    return settingsRepository.save(
                            UserNotificationSettings.builder()
                                    .user(user)
                                    .dailyDigestEnabled(true)
                                    .alertEmailsEnabled(true)
                                    .maintenanceRemindersEnabled(true)
                                    .build()
                    );
                });
    }

    private NotificationSettingsResponse toResponse(UserNotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.isDailyDigestEnabled(),
                settings.isAlertEmailsEnabled(),
                settings.isMaintenanceRemindersEnabled()
        );
    }
}