CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       first_name VARCHAR(100),
                       last_name VARCHAR(100),
                       role VARCHAR(50) NOT NULL DEFAULT 'OWNER',
                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at TIMESTAMP NOT NULL DEFAULT now()
);