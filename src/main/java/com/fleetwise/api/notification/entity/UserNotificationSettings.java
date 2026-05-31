package com.fleetwise.api.notification.entity;

import com.fleetwise.api.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_notification_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSettings {

    @Id
    @Column(name = "user_id")
    private java.util.UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "daily_digest_enabled", nullable = false)
    private boolean dailyDigestEnabled;

    @Column(name = "alert_emails_enabled", nullable = false)
    private boolean alertEmailsEnabled;

    @Column(name = "maintenance_reminders_enabled", nullable = false)
    private boolean maintenanceRemindersEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}