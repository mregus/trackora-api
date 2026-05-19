CREATE TABLE vehicle_documents (
                                   id UUID PRIMARY KEY,
                                   vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
                                   original_file_name VARCHAR(255) NOT NULL,
                                   stored_file_name VARCHAR(255) NOT NULL,
                                   content_type VARCHAR(100),
                                   file_size BIGINT NOT NULL,
                                   storage_path VARCHAR(500) NOT NULL,
                                   document_type VARCHAR(50),
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_documents_vehicle_id
    ON vehicle_documents(vehicle_id);