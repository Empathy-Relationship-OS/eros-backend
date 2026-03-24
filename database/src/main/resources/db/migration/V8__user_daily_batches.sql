-- V8: User Daily Batches Table
-- Tracks daily batch fetches per user for rate limiting (max 3 batches per day)
-- Resets daily to allow fresh batch counts

-- User daily batches table: Records batch fetch counts per user per day
CREATE TABLE IF NOT EXISTS user_daily_batches (
    -- Primary key - composite of user and date
    user_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    batch_date DATE NOT NULL,

    -- Batch tracking
    batch_count INT NOT NULL DEFAULT 0 CHECK (batch_count >= 0 AND batch_count <= 3),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    PRIMARY KEY (user_id, batch_date)
);

-- Index for efficient user lookups
CREATE INDEX idx_user_daily_batches_user_id ON user_daily_batches(user_id);
CREATE INDEX idx_user_daily_batches_date ON user_daily_batches(batch_date);

-- Comments for documentation
COMMENT ON TABLE user_daily_batches IS 'Tracks daily batch fetch counts per user for rate limiting (max 3 batches per day)';
COMMENT ON COLUMN user_daily_batches.user_id IS 'User who fetched batches (references users table)';
COMMENT ON COLUMN user_daily_batches.batch_date IS 'Date of batch fetches (UTC)';
COMMENT ON COLUMN user_daily_batches.batch_count IS 'Number of batches fetched on this date (0-3)';
COMMENT ON COLUMN user_daily_batches.created_at IS 'When the first batch was fetched for this date';
COMMENT ON COLUMN user_daily_batches.updated_at IS 'When the record was last updated';
