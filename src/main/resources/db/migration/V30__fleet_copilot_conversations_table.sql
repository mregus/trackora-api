CREATE TABLE fleet_copilot_conversations (
                                             id UUID PRIMARY KEY,
                                             fleet_id UUID NOT NULL REFERENCES fleets(id),
                                             user_id UUID NOT NULL REFERENCES users(id),
                                             title VARCHAR(200) NOT NULL,
                                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE fleet_copilot_messages (
                                        id UUID PRIMARY KEY,
                                        conversation_id UUID NOT NULL
                                            REFERENCES fleet_copilot_conversations(id)
                                                ON DELETE CASCADE,
                                        role VARCHAR(20) NOT NULL,
                                        content TEXT NOT NULL,
                                        supporting_facts TEXT,
                                        ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
                                        created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_copilot_conversation_fleet_user
    ON fleet_copilot_conversations(fleet_id, user_id, updated_at DESC);

CREATE INDEX idx_copilot_message_conversation
    ON fleet_copilot_messages(conversation_id, created_at);