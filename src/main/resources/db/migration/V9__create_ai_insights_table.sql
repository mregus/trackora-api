CREATE TABLE ai_insights (
                             id UUID PRIMARY KEY,
                             fleet_id UUID NOT NULL REFERENCES fleets(id) ON DELETE CASCADE,
                             prompt_hash VARCHAR(64),
                             summary TEXT NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_insights_fleet_id ON ai_insights(fleet_id);