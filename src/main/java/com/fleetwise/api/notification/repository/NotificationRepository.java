package com.fleetwise.api.notification.repository;

import com.fleetwise.api.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);
}