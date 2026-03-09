-- Initial migration placeholder
-- This establishes the baseline for Flyway versioning
-- Actual table creation will be done in subsequent migrations (V1, V2, etc.)

-- Create PostgreSQL extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone to UTC for consistent timestamp behavior
SET timezone = 'UTC';
