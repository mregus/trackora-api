CREATE TABLE fuel_logs (
                           id UUID PRIMARY KEY,
                           vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
                           fuel_date DATE NOT NULL,
                           mileage INT,
                           gallons DECIMAL(10,2) NOT NULL,
                           total_cost DECIMAL(10,2) NOT NULL,
                           price_per_gallon DECIMAL(10,3) GENERATED ALWAYS AS (total_cost / NULLIF(gallons,0)) STORED,
                           notes TEXT,
                           created_at TIMESTAMP NOT NULL DEFAULT now(),
                           updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_fuel_logs_vehicle_id ON fuel_logs(vehicle_id);
