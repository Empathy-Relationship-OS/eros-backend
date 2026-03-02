-- V3: User Preferences Tables
-- Creates tables for user dating preferences with a 1-1 relationship.
-- Junction tables handle user city preference

-- Main user preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id VARCHAR(128) PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,

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
    date_limit INTEGER,
    reach_level VARCHAR(128) NOT NULL,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT user_preferences_age_min_valid CHECK (age_range_min > 17),
    CONSTRAINT user_preferences_age_max_greater CHECK (age_range_max > age_range_min),
    CONSTRAINT user_preferences_height_range_valid CHECK (height_range_max > height_range_min)
);


-- Junction table: User city preferences
CREATE TABLE IF NOT EXISTS user_cities_preference (
    user_id VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    city_id BIGINT NOT NULL REFERENCES cities(city_id) ON DELETE CASCADE,
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, city_id)
);
-- Indexes for junction table
CREATE INDEX IF NOT EXISTS idx_user_cities_preference_city_id ON user_cities_preference(city_id);

-- Comments for documentation
COMMENT ON TABLE user_preferences IS 'Main user dating preferences (one record per user)';
COMMENT ON TABLE user_cities_preference IS 'Many-to-many: User preferred cities for dating';

COMMENT ON COLUMN user_preferences.date_ethnicity IS 'List of ethnicity preference';
COMMENT ON COLUMN user_preferences.date_activities IS 'List of activities users prefer';
COMMENT ON COLUMN user_preferences.date_languages IS 'List of languages users prefer';
COMMENT ON COLUMN user_preferences.date_limit IS 'Number of dates user wants per ?';
COMMENT ON COLUMN user_preferences.age_range_min IS 'Minimum age preference (must be 18+)';
COMMENT ON COLUMN user_preferences.age_range_max IS 'Maximum age preference (must be > min)';
COMMENT ON COLUMN user_preferences.height_range_min IS 'Minimum height preference in cm';
COMMENT ON COLUMN user_preferences.height_range_max IS 'Maximum height preference in cm';
COMMENT ON COLUMN user_preferences.reach_level IS 'How strict the preferences are used for matching';

