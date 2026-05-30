CREATE TABLE activity_logs (
                               id UUID PRIMARY KEY,
                               fleet_id UUID REFERENCES fleets(id) ON DELETE CASCADE,
                               vehicle_id UUID REFERENCES vehicles(id) ON DELETE SET NULL,
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               action VARCHAR(100) NOT NULL,
                               entity_type VARCHAR(50) NOT NULL,
                               entity_id UUID,
                               message VARCHAR(500) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_fleet_id_created_at
    ON activity_logs(fleet_id, created_at DESC);

CREATE INDEX idx_activity_logs_user_id_created_at
    ON activity_logs(user_id, created_at DESC);