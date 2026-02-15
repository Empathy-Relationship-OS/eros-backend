-- V2: User Preferences Tables
-- Creates tables for user dating preferences with normalized many-to-many relationships
-- Junction tables handle user city preference

-- Main user preferences table
CREATE TABLE user_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,

    -- Who I like section
    gender_identities TEXT[] NOT NULL,
    age_range_min INTEGER NOT NULL,
    age_range_max INTEGER NOT NULL,
    height_range_min INTEGER NOT NULL,  -- In cm
    height_range_max INTEGER NOT NULL,  -- In cm
    date_ethnicity TEXT[] NOT NULL,

    -- Dating practicalities
    date_languages TEXT[] NOT NULL,
    date_activities TEXT[] NOT NULL,
    date_limit INTEGER NOT NULL,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT user_preferences_date_limit_range CHECK (date_limit BETWEEN 1 AND 6),
    CONSTRAINT user_preferences_age_min_valid CHECK (age_range_min > 17),
    CONSTRAINT user_preferences_age_max_greater CHECK (age_range_max > age_range_min),
    CONSTRAINT user_preferences_height_range_valid CHECK (height_range_max > height_range_min)
);

-- Indexes for user_preferences
CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX idx_user_preferences_age_range ON user_preferences(age_range_min, age_range_max);

-- Junction table: User city preferences
CREATE TABLE user_city_preferences (
    user_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    city_id BIGSERIAL NOT NULL REFERENCES cities(id) ON DELETE CASCADE,
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, city_id)
);
-- Indexes for junction table
CREATE INDEX idx_user_city_preferences_city_id ON user_city_preferences(city_id);

-- Comments for documentation
COMMENT ON TABLE user_preferences IS 'Main user dating preferences (one record per user)';
COMMENT ON TABLE user_city_preferences IS 'Many-to-many: User preferred cities for dating';

COMMENT ON COLUMN user_preferences.date_ethnicity IS 'List of ethnicity preference';
COMMENT ON COLUMN user_preferences.date_activities IS 'List of activities users prefer';
COMMENT ON COLUMN user_preferences.date_languages IS 'List of languages users prefer';
COMMENT ON COLUMN user_preferences.date_limit IS 'Number of dates user wants per ?';
COMMENT ON COLUMN user_preferences.age_range_min IS 'Minimum age preference (must be 18+)';
COMMENT ON COLUMN user_preferences.age_range_max IS 'Maximum age preference (must be > min)';
COMMENT ON COLUMN user_preferences.height_range_min IS 'Minimum height preference in cm';
COMMENT ON COLUMN user_preferences.height_range_max IS 'Maximum height preference in cm';

