CREATE TABLE fleets (
                        id UUID PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        owner_user_id UUID NOT NULL REFERENCES users(id),
                        created_at TIMESTAMP NOT NULL DEFAULT now(),
                        updated_at TIMESTAMP NOT NULL DEFAULT now()
);