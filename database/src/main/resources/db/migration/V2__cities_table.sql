-- Master table: Cities
CREATE TABLE cities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for master tables (name lookups)
CREATE INDEX idx_cities_name ON cities(name);

-- Comments for columns or table
COMMENT ON TABLE cities IS 'List of all cities available for dating preferences';
COMMENT ON COLUMN cities.name IS 'Name of a city containing an activity';