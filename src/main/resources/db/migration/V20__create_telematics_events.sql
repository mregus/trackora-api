CREATE TABLE telematics_events (
                                   id UUID PRIMARY KEY,
                                   vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,

                                   recorded_at TIMESTAMP NOT NULL,

                                   latitude DOUBLE PRECISION,
                                   longitude DOUBLE PRECISION,

                                   speed_mph NUMERIC(8,2),
                                   odometer_miles NUMERIC(10,2),
                                   fuel_level_percent NUMERIC(5,2),
                                   engine_temp_f NUMERIC(6,2),
                                   battery_voltage NUMERIC(5,2),

                                   check_engine BOOLEAN NOT NULL DEFAULT FALSE,
                                   harsh_braking BOOLEAN NOT NULL DEFAULT FALSE,
                                   idle_minutes INTEGER NOT NULL DEFAULT 0,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_telematics_vehicle_recorded_at
    ON telematics_events(vehicle_id, recorded_at DESC);