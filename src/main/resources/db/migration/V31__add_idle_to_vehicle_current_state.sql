ALTER TABLE vehicle_current_state
    ADD COLUMN ignition_on BOOLEAN,
    ADD COLUMN idle_started_at TIMESTAMPTZ,
    ADD COLUMN current_idle_minutes INTEGER NOT NULL DEFAULT 0;