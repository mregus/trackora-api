CREATE TABLE geometris_raw_packets (
                                       id UUID PRIMARY KEY,

                                       serial_number VARCHAR(100),
                                       reason_text VARCHAR(100),

                                       raw_packet TEXT NOT NULL,

                                       parsed_successfully BOOLEAN NOT NULL,

                                       error_message TEXT,

                                       received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_geometris_raw_packets_serial_number
    ON geometris_raw_packets(serial_number);

CREATE INDEX idx_geometris_raw_packets_received_at
    ON geometris_raw_packets(received_at DESC);