ALTER TABLE vehicles
ADD COLUMN telematics_device_serial VARCHAR(50);

CREATE INDEX idx_vehicle_telematics_serial
ON vehicles(telematics_device_serial);