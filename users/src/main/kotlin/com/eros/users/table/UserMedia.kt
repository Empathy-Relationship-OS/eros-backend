package com.eros.users.table


import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * User media table for storing photos and videos
 * 
 * Requirements:
 * - Minimum 3 media items per user
 * - Maximum 6 media items per user
 * - User-defined ordering (displayOrder 1-6)
 * - One primary photo/video
 * - S3 URLs stored
 */
object UserMedia : Table("user_media") {
    // Primary key
    val id = long("id").autoIncrement()
    
    // Foreign key to Users table
    val userId = varchar("user_id", 128).references(Users.userId)
    
    // Media details
    val mediaUrl = text("media_url") // S3 URL
    val mediaType = varchar("media_type", 10) // MediaType enum: PHOTO, VIDEO
    
    // Ordering and priority
    val displayOrder = integer("display_order") // 1-6, user-defined order
    val isPrimary = bool("is_primary").default(false) // First photo shown

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
    
    init {
        // Ensure only one primary media per user
        uniqueIndex("unique_primary_media_per_user", userId, isPrimary) {
            isPrimary eq true
        }
        
        // Ensure displayOrder is unique per user
        uniqueIndex("unique_display_order_per_user", userId, displayOrder)
        
        // Check constraint: displayOrder must be between 1 and 6
        check("display_order_range") { displayOrder.between(1, 6) }
    }
}
