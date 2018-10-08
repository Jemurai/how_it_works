CREATE EXTENSION pgcrypto;

CREATE TABLE IF NOT EXISTS tokens (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             INT  NOT NULL,
    encrypted_secret    TEXT NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);