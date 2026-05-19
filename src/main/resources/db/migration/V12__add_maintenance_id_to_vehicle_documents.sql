ALTER TABLE vehicle_documents
    ADD COLUMN maintenance_id UUID REFERENCES maintenance(id) ON DELETE CASCADE;

CREATE INDEX idx_vehicle_documents_maintenance_id
    ON vehicle_documents(maintenance_id);