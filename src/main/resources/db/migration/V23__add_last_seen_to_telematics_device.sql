ALTER TABLE telematics_devices
ADD COLUMN last_seen_at TIMESTAMP;

CREATE INDEX idx_telematics_devices_last_seen_at
ON telematics_devices(last_seen_at);