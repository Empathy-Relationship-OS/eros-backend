-- Master table: Cities
CREATE TABLE IF NOT EXISTS cities (
    city_id BIGSERIAL PRIMARY KEY,
    city_name VARCHAR(128) NOT NULL UNIQUE,

    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_city_latitude_range CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_city_longitude_range CHECK (longitude BETWEEN -180 AND 180)
);

-- Indexes for master tables (name lookups)
CREATE INDEX idx_cities_name ON cities(city_name);

-- Comments for columns or table
COMMENT ON TABLE cities IS 'List of all cities available for dating preferences';
COMMENT ON COLUMN cities.city_name IS 'Name of a city containing an activity';
COMMENT ON COLUMN cities.longitude IS 'Longitude of the city centre';
COMMENT ON COLUMN cities.latitude IS 'Latitude of the city centre';