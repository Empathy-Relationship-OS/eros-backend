-- V1: Authentication Tables
-- Creates core authentication and identity tables for the auth module
-- Auth module owns: identity, credentials, verification (who you are)
-- User profiles will be in V2 (users module)

-- Users table: Core identity and authentication
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMP,

    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_phone_unique UNIQUE (phone),
    CONSTRAINT users_verification_status_check CHECK (verification_status IN ('PENDING', 'VERIFIED', 'SUSPENDED'))
);

-- Indexes for common query patterns
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_users_verification_status ON users(verification_status);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_last_active_at ON users(last_active_at) WHERE last_active_at IS NOT NULL;

-- OTP Verification table: Phone number verification via OTP
CREATE TABLE otp_verification (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT otp_phone_number_unique UNIQUE (phone_number),
    CONSTRAINT otp_attempts_check CHECK (attempts >= 0 AND attempts <= 10)
);

-- Indexes for OTP verification queries
CREATE INDEX idx_otp_phone_number ON otp_verification(phone_number);
CREATE INDEX idx_otp_expires_at ON otp_verification(expires_at);

-- Comments for documentation
COMMENT ON TABLE users IS 'Core user identity and authentication data';
COMMENT ON COLUMN users.id IS 'Unique user identifier (UUID v4)';
COMMENT ON COLUMN users.email IS 'User email address (unique, required for authentication)';
COMMENT ON COLUMN users.phone IS 'User phone number (optional, unique if provided)';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.verification_status IS 'User verification state: PENDING, VERIFIED, or SUSPENDED';
COMMENT ON COLUMN users.created_at IS 'Account creation timestamp (UTC)';
COMMENT ON COLUMN users.updated_at IS 'Last update timestamp for user record (UTC)';
COMMENT ON COLUMN users.last_active_at IS 'Last activity timestamp for session management (UTC, nullable)';

COMMENT ON TABLE otp_verification IS 'OTP verification records for phone number confirmation';
COMMENT ON COLUMN otp_verification.phone_number IS 'Phone number being verified';
COMMENT ON COLUMN otp_verification.otp_hash IS 'Hashed OTP code (never store plaintext)';
COMMENT ON COLUMN otp_verification.expires_at IS 'OTP expiration timestamp (typically 5-10 minutes)';
COMMENT ON COLUMN otp_verification.attempts IS 'Number of verification attempts (max 10)';
