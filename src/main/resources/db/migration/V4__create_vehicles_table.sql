CREATE TABLE vehicles (
                          id UUID PRIMARY KEY,
                          fleet_id UUID NOT NULL REFERENCES fleets(id) ON DELETE CASCADE,

                          vin VARCHAR(50),
                          make VARCHAR(100) NOT NULL,
                          model VARCHAR(100) NOT NULL,
                          year INT NOT NULL,

                          license_plate VARCHAR(50),

                          current_mileage INT,

                          status VARCHAR(50) NOT NULL,

                          created_at TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vehicles_fleet_id
    ON vehicles(fleet_id);