-- V7: Matching Tables
-- Creates the matches table for the matching system
-- Tracks potential matches between users with like/pass actions and serving timestamps

-- Matches table: Records match candidates between two users
CREATE TABLE IF NOT EXISTS matches (
    -- Primary key - auto-incrementing unique ID
    match_id BIGSERIAL PRIMARY KEY,

    -- Foreign keys to users table
    user1_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    user2_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,

    -- Match data
    liked BOOLEAN,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    served_at TIMESTAMP,

    -- Constraints
    CONSTRAINT matches_different_users CHECK (user1_id != user2_id),
    CONSTRAINT matches_user_pair_unique UNIQUE (user1_id, user2_id)
);

-- Indexes for common query patterns
CREATE INDEX idx_matches_user1_id ON matches(user1_id);
CREATE INDEX idx_matches_user2_id ON matches(user2_id);

-- Composite index for the unique user pair (already covered by UNIQUE constraint, but explicit for query optimization)
CREATE INDEX idx_matches_user_pair ON matches(user1_id, user2_id);

-- Comments for documentation
COMMENT ON TABLE matches IS 'Match candidates between users with like/pass tracking and serving timestamps';
COMMENT ON COLUMN matches.match_id IS 'Auto-incrementing primary key';
COMMENT ON COLUMN matches.user1_id IS 'First user in the match pair (references users table)';
COMMENT ON COLUMN matches.user2_id IS 'Second user in the match pair (references users table)';
COMMENT ON COLUMN matches.liked IS 'Whether user1 liked user2';
COMMENT ON COLUMN matches.created_at IS 'When the match record was created';
COMMENT ON COLUMN matches.updated_at IS 'When the match record was last updated';
COMMENT ON COLUMN matches.served_at IS 'When the match was served to the user (nullable)';
