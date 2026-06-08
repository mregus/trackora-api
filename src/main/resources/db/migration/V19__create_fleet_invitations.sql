CREATE TABLE fleet_invitations (
                                   id UUID PRIMARY KEY,

                                   fleet_id UUID NOT NULL
                                       REFERENCES fleets(id)
                                           ON DELETE CASCADE,

                                   email VARCHAR(255) NOT NULL,

                                   role VARCHAR(50) NOT NULL,

                                   token VARCHAR(255) NOT NULL UNIQUE,

                                   accepted BOOLEAN NOT NULL DEFAULT FALSE,

                                   expires_at TIMESTAMP NOT NULL,

                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fleet_invitation_email
    ON fleet_invitations(email);

CREATE INDEX idx_fleet_invitation_token
    ON fleet_invitations(token);