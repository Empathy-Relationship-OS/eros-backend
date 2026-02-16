-- V1: User Tables (Firebase Integration + Profile Data)
-- Creates comprehensive user profile table synced with Firebase Auth
-- Firebase handles: passwords, OTP verification, email/phone verification, JWT tokens
-- Backend handles: user profile data, business logic, Firebase UID as primary key

-- Users table: Complete user profile using Firebase UID as primary key
CREATE TABLE users (
    -- Primary key - Firebase user ID
    user_id VARCHAR(128) PRIMARY KEY,

    -- Required fields
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    height_cm INTEGER NOT NULL,
    date_of_birth DATE NOT NULL,
    city VARCHAR(100) NOT NULL,
    education_level VARCHAR(50) NOT NULL,
    gender VARCHAR(50) NOT NULL,

    -- Optional profile fields
    occupation VARCHAR(100),
    bio VARCHAR(300),

    -- Hobbies & Interests (PostgreSQL TEXT[] array)
    -- Combined: Activity, Interest, Entertainment, Creative, MusicGenre, FoodAndDrink, Sport
    -- Min 5, Max 10
    interests TEXT[] NOT NULL,

    -- Personality Traits (PostgreSQL TEXT[] array)
    -- Min 3, Max 10
    traits TEXT[] NOT NULL,

    -- Languages
    preferred_language VARCHAR(50) NOT NULL,
    spoken_languages TEXT[] NOT NULL,
    spoken_languages_display BOOLEAN NOT NULL DEFAULT FALSE,

    -- Beliefs & Values
    religion VARCHAR(50),
    religion_display BOOLEAN NOT NULL DEFAULT FALSE,
    political_view VARCHAR(50),
    political_view_display BOOLEAN NOT NULL DEFAULT FALSE,

    -- Habits
    alcohol_consumption VARCHAR(50),
    alcohol_consumption_display BOOLEAN NOT NULL DEFAULT FALSE,
    smoking_status VARCHAR(50),
    smoking_status_display BOOLEAN NOT NULL DEFAULT FALSE,
    diet VARCHAR(50),
    diet_display BOOLEAN NOT NULL DEFAULT FALSE,

    -- Relationship goals
    date_intentions VARCHAR(50),
    date_intentions_display BOOLEAN NOT NULL DEFAULT FALSE,
    relationship_type VARCHAR(50),
    relationship_type_display BOOLEAN NOT NULL DEFAULT FALSE,
    kids_preference VARCHAR(50),
    kids_preference_display BOOLEAN NOT NULL DEFAULT FALSE,

    -- Identity
    sexual_orientation VARCHAR(50),
    sexual_orientation_display BOOLEAN NOT NULL DEFAULT FALSE,
    pronouns VARCHAR(50),
    pronouns_display BOOLEAN NOT NULL DEFAULT FALSE,
    star_sign VARCHAR(50),
    star_sign_display BOOLEAN NOT NULL DEFAULT FALSE,
    ethnicity TEXT[] NOT NULL,
    ethnicity_display BOOLEAN NOT NULL DEFAULT FALSE,

    -- Brain & Body attributes (PostgreSQL TEXT[] arrays)
    brain_attributes TEXT[],
    brain_attributes_display BOOLEAN NOT NULL DEFAULT FALSE,
    brain_description VARCHAR(100),
    brain_description_display BOOLEAN NOT NULL DEFAULT FALSE,
    body_attributes TEXT[],
    body_attributes_display BOOLEAN NOT NULL DEFAULT FALSE,
    body_description VARCHAR(100),
    body_description_display BOOLEAN NOT NULL DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_height_positive CHECK (height_cm > 0),
    CONSTRAINT users_interests_count CHECK (array_length(interests, 1) BETWEEN 5 AND 10),
    CONSTRAINT users_traits_count CHECK (array_length(traits, 1) BETWEEN 3 AND 10),
    CONSTRAINT users_bio_length CHECK (bio IS NULL OR length(bio) <= 300),
    CONSTRAINT users_brain_desc_length CHECK (brain_description IS NULL OR length(brain_description) <= 100),
    CONSTRAINT users_body_desc_length CHECK (body_description IS NULL OR length(body_description) <= 100)
);

-- Indexes for common query patterns
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_city ON users(city);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_gender ON users(gender);
CREATE INDEX idx_users_date_of_birth ON users(date_of_birth);

-- GIN indexes for array columns (efficient searching within arrays)
CREATE INDEX idx_users_interests ON users USING GIN(interests);
CREATE INDEX idx_users_traits ON users USING GIN(traits);
CREATE INDEX idx_users_spoken_languages ON users USING GIN(spoken_languages);
CREATE INDEX idx_users_ethnicity ON users USING GIN(ethnicity);

-- Comments for documentation
COMMENT ON TABLE users IS 'Complete user profile linked to Firebase Auth (Firebase UID as PK)';
COMMENT ON COLUMN users.user_id IS 'Firebase Authentication user ID (primary key from Firebase Auth)';
COMMENT ON COLUMN users.email IS 'User email address (unique, synced from Firebase)';
COMMENT ON COLUMN users.interests IS 'User interests array (5-10 items required)';
COMMENT ON COLUMN users.traits IS 'Personality traits array (3-10 items required)';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp (NULL = active user)';
