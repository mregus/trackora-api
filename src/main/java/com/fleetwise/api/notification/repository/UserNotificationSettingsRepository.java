package com.fleetwise.api.notification.repository;

import com.fleetwise.api.notification.entity.UserNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserNotificationSettingsRepository
        extends JpaRepository<UserNotificationSettings, UUID> {
}