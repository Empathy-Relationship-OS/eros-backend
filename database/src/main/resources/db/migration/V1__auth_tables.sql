-- V1: Authentication Tables (Firebase Integration)
-- Creates core user identity table synced with Firebase Auth
-- Firebase handles: passwords, OTP verification, email/phone verification, JWT tokens
-- Backend handles: user profile data, business logic, Firebase UID as primary key

-- Users table: User identity using Firebase UID as primary key
CREATE TABLE users (
    id VARCHAR(128) PRIMARY KEY,  -- Firebase UID (e.g., "abc123def456...")
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP,

    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_phone_unique UNIQUE (phone)
);

-- Indexes for common query patterns
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_active_at ON users(last_active_at) WHERE last_active_at IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE users IS 'Core user identity linked to Firebase Auth (Firebase UID as PK)';
COMMENT ON COLUMN users.id IS 'Firebase Authentication user ID (primary key from Firebase Auth)';
COMMENT ON COLUMN users.email IS 'User email address (unique, synced from Firebase)';
COMMENT ON COLUMN users.phone IS 'User phone number (optional, synced from Firebase)';
COMMENT ON COLUMN users.created_at IS 'Account creation timestamp (UTC)';
COMMENT ON COLUMN users.updated_at IS 'Last update timestamp for user record (UTC)';
COMMENT ON COLUMN users.last_active_at IS 'Last activity timestamp for session management (UTC, nullable)';
