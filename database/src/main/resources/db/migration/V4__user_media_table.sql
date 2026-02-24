-- V4: User Media Table
-- Stores media URLs for user photos (and future video support).
-- media_url stores the canonical public URL (S3 or CDN) — implementation-agnostic.
-- thumbnail_url is populated asynchronously after Lambda processing.
-- Display order: 1-6 (user-controlled ordering).
-- One primary photo per user enforced via partial unique index.

CREATE TABLE IF NOT EXISTS user_media (
    id             BIGSERIAL PRIMARY KEY,
    user_id        VARCHAR(128) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,

    -- Public URLs (S3 or CDN) — not storage-implementation-specific keys
    media_url      TEXT        NOT NULL,
    thumbnail_url  TEXT,                -- populated asynchronously after Lambda processing

    media_type     VARCHAR(10) NOT NULL DEFAULT 'PHOTO',  -- MediaType enum: PHOTO, VIDEO
    display_order  INTEGER     NOT NULL,                  -- 1-6, user-defined ordering
    is_primary     BOOLEAN     NOT NULL DEFAULT FALSE,

    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Display order must be 1-6
    CONSTRAINT user_media_display_order_range CHECK (display_order BETWEEN 1 AND 6),

    -- Unique display order per user
    CONSTRAINT user_media_unique_display_order UNIQUE (user_id, display_order)
);

-- Only one primary photo per user (partial unique index)
CREATE UNIQUE INDEX idx_user_media_unique_primary
    ON user_media (user_id)
    WHERE is_primary = TRUE;


COMMENT ON TABLE user_media IS 'User photos (and future videos). Max 6 per user.';
COMMENT ON COLUMN user_media.media_url IS 'Public URL for the original file (S3 or CDN)';
COMMENT ON COLUMN user_media.thumbnail_url IS 'Public URL for the 300x300 thumbnail, written by Lambda after upload';
COMMENT ON COLUMN user_media.display_order IS 'User-defined display position (1 = first shown, max 6)';
COMMENT ON COLUMN user_media.is_primary IS 'TRUE for the single photo shown as profile picture';
