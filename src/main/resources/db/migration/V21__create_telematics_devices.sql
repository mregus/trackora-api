CREATE TABLE telematics_devices (
                                    id UUID PRIMARY KEY,
                                    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,

                                    provider VARCHAR(50) NOT NULL,
                                    external_device_id VARCHAR(100) NOT NULL,
                                    serial_number VARCHAR(100),
                                    imei VARCHAR(50),
                                    vin VARCHAR(50),

                                    active BOOLEAN NOT NULL DEFAULT TRUE,

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT uq_telematics_provider_external_device
                                        UNIQUE (provider, external_device_id)
);

CREATE INDEX idx_telematics_devices_vehicle_id
    ON telematics_devices(vehicle_id);

CREATE INDEX idx_telematics_devices_serial_number
    ON telematics_devices(serial_number);

CREATE INDEX idx_telematics_devices_vin
    ON telematics_devices(vin);