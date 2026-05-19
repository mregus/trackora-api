CREATE TABLE alerts (
                        id UUID PRIMARY KEY,
                        fleet_id UUID NOT NULL REFERENCES fleets(id) ON DELETE CASCADE,
                        vehicle_id UUID REFERENCES vehicles(id) ON DELETE SET NULL,
                        type VARCHAR(50) NOT NULL,
                        message TEXT NOT NULL,
                        resolved BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP NOT NULL DEFAULT now(),
                        resolved_at TIMESTAMP
);

CREATE INDEX idx_alerts_fleet_id ON alerts(fleet_id);
CREATE INDEX idx_alerts_vehicle_id ON alerts(vehicle_id);
