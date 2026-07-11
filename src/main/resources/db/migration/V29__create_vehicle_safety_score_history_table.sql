CREATE TABLE vehicle_safety_score_history (
                                              id UUID PRIMARY KEY,
                                              vehicle_id UUID NOT NULL REFERENCES vehicles(id),
                                              score_date DATE NOT NULL,
                                              score INTEGER NOT NULL,
                                              hard_brakes INTEGER NOT NULL DEFAULT 0,
                                              hard_accelerations INTEGER NOT NULL DEFAULT 0,
                                              harsh_turns INTEGER NOT NULL DEFAULT 0,
                                              speeding_events INTEGER NOT NULL DEFAULT 0,
                                              idle_minutes INTEGER NOT NULL DEFAULT 0,
                                              check_engine BOOLEAN NOT NULL DEFAULT FALSE,
                                              miles_driven NUMERIC(12, 2),
                                              recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                              CONSTRAINT uk_vehicle_safety_score_history_day
                                                  UNIQUE (vehicle_id, score_date)
);

CREATE INDEX idx_safety_history_vehicle_date
    ON vehicle_safety_score_history(vehicle_id, score_date DESC);

CREATE INDEX idx_safety_history_date
    ON vehicle_safety_score_history(score_date);