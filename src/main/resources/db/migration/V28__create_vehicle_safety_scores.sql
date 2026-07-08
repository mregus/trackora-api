CREATE TABLE vehicle_safety_scores (
                                       id UUID PRIMARY KEY,
                                       vehicle_id UUID NOT NULL UNIQUE REFERENCES vehicles(id),
                                       score INTEGER NOT NULL,
                                       hard_brakes INTEGER NOT NULL DEFAULT 0,
                                       hard_accelerations INTEGER NOT NULL DEFAULT 0,
                                       harsh_turns INTEGER NOT NULL DEFAULT 0,
                                       speeding_events INTEGER NOT NULL DEFAULT 0,
                                       idle_minutes INTEGER NOT NULL DEFAULT 0,
                                       check_engine BOOLEAN NOT NULL DEFAULT FALSE,
                                       miles_driven NUMERIC(10,2),
                                       updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_vehicle_safety_scores_score
    ON vehicle_safety_scores(score);