CREATE TABLE fleet_members (
                               id UUID PRIMARY KEY,
                               fleet_id UUID NOT NULL REFERENCES fleets(id) ON DELETE CASCADE,
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               role VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT uq_fleet_members_fleet_user UNIQUE (fleet_id, user_id)
);

CREATE INDEX idx_fleet_members_fleet_id
    ON fleet_members(fleet_id);

CREATE INDEX idx_fleet_members_user_id
    ON fleet_members(user_id);