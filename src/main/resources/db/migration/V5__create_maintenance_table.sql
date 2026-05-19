CREATE TABLE maintenance (
                             id UUID PRIMARY KEY,
                             vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
                             service_type VARCHAR(100) NOT NULL,
                             description TEXT,
                             service_date DATE NOT NULL,
                             mileage INT,
                             cost DECIMAL(10,2),
                             vendor VARCHAR(100),
                             next_service_date DATE,
                             created_at TIMESTAMP NOT NULL DEFAULT now(),
                             updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_maintenance_vehicle_id
    ON maintenance(vehicle_id);
