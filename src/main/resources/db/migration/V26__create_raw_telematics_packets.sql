CREATE TABLE raw_telematics_packets (
                                        id UUID PRIMARY KEY,
                                        device_serial VARCHAR(100),
                                        packet_type VARCHAR(50),
                                        raw_payload TEXT NOT NULL,
                                        processed BOOLEAN NOT NULL DEFAULT FALSE,
                                        error_message TEXT,
                                        received_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_raw_telematics_packets_device_serial
    ON raw_telematics_packets(device_serial);

CREATE INDEX idx_raw_telematics_packets_received_at
    ON raw_telematics_packets(received_at);