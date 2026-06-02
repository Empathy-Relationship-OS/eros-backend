-- The following table defines all the required information for a business that will host a date.
CREATE TABLE IF NOT EXISTS venues (

    -- Basic information.
    venue_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(128) NOT NULL,

    -- Location of business
    address VARCHAR(128) NOT NULL,
    city_id BIGSERIAL REFERENCES city(city_id) ON DELETE CASCADE NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    -- General information regarding the business.
    price_range VARCHAR(16) NOT NULL,
    max_capacity INTEGER NOT NULL,
    reservation_required BOOLEAN NOT NULL,
    partner_instructions VARCHAR(256) NOT NULL,
    disabled_friendly BOOLEAN NOT NULL,
    indoor_outdoor VARCHAR(16) NOT NULL,
    parking_available BOOLEAN NOT NULL,

    website_url VARCHAR(256),
    dress_code VARCHAR(16) NOT NULL,

    active_from DATE NOT NULL,
    active_to DATE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()

);

--todo: implement constraints and indexes.