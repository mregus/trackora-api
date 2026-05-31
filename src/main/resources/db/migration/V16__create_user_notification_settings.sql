CREATE TABLE user_notification_settings (
                                            user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                            daily_digest_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                            alert_emails_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                            maintenance_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);