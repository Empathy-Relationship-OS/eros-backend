-- V9: User Marketing Consent Table
-- Creates the user_marketing_consent table for storing user marketing preferences
-- Tracks whether users have opted in to receive marketing communications

-- User Marketing Consent table: Records user's marketing communication preferences
CREATE TABLE IF NOT EXISTS user_marketing_consent (
    -- Primary key - references users table
    user_id VARCHAR(128) PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,

    -- Marketing consent flag
    marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Comments for documentation
COMMENT ON TABLE user_marketing_consent IS 'User marketing communication preferences and consent tracking';
COMMENT ON COLUMN user_marketing_consent.user_id IS 'User identifier (references users table)';
COMMENT ON COLUMN user_marketing_consent.marketing_consent IS 'Whether user has consented to receive marketing communications';
COMMENT ON COLUMN user_marketing_consent.created_at IS 'When the consent record was created';
COMMENT ON COLUMN user_marketing_consent.updated_at IS 'When the consent record was last updated';
