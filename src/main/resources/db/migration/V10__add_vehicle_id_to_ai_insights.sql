ALTER TABLE ai_insights
    ADD COLUMN vehicle_id UUID REFERENCES vehicles(id) ON DELETE CASCADE;

CREATE INDEX idx_ai_insights_vehicle_id
    ON ai_insights(vehicle_id);