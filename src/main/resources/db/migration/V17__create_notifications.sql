CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               title VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               read BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_created_at
    ON notifications(user_id, created_at DESC);

CREATE INDEX idx_notifications_user_read
    ON notifications(user_id, read);