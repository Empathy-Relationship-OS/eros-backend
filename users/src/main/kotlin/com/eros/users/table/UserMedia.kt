package com.eros.users.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.Instant

/**
 * User media table for storing photos (and future videos).
 *
 * Design:
 * - [mediaUrl] stores the canonical public URL (S3 direct or CDN) — implementation-agnostic.
 *   URL construction is the responsibility of the service layer (PhotoService), not the DB layer.
 * - [thumbnailUrl] is nullable: populated asynchronously by a Lambda triggered on S3 upload.
 * - Maximum 6 media items per user, display_order 1-6.
 * - Exactly one primary photo enforced via partial unique index in migration V4.
 */
object UserMedia : Table("user_media") {

    val id           = long("id").autoIncrement()
    val userId       = varchar("user_id", 128).references(Users.userId)

    /** Canonical public URL for the original file (S3 or CDN) */
    val mediaUrl     = text("media_url")

    /** Canonical public URL for the 300x300 thumbnail — written by Lambda, null until ready */
    val thumbnailUrl = text("thumbnail_url").nullable()

    val mediaType    = varchar("media_type", 10)   // MediaType enum value
    val displayOrder = integer("display_order")     // 1-6
    val isPrimary    = bool("is_primary").default(false)

    val createdAt    = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt    = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        // Unique display order per user (mirrors DB constraint in migration V4)
        uniqueIndex("user_media_unique_display_order", userId, displayOrder)
    }
}
