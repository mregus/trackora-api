CREATE TABLE vehicle_current_state (
                                       vehicle_id UUID PRIMARY KEY REFERENCES vehicles(id),
                                       latitude DOUBLE PRECISION,
                                       longitude DOUBLE PRECISION,
                                       speed_mph NUMERIC(10,2),
                                       fuel_level_percent NUMERIC(5,2),
                                       heading_degrees INTEGER,
                                       check_engine BOOLEAN NOT NULL DEFAULT FALSE,
                                       last_seen_at TIMESTAMP NOT NULL,
                                       updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_vehicle_current_state_last_seen
    ON vehicle_current_state(last_seen_at);