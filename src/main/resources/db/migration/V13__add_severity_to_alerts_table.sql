ALTER TABLE alerts
    ADD COLUMN severity VARCHAR(20) NOT NULL DEFAULT 'INFO';

CREATE INDEX idx_alerts_severity
    ON alerts(severity);