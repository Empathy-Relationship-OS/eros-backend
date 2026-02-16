package com.eros.users.models

import com.eros.common.serializers.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * User media domain model for photos and videos
 */
@Serializable
data class UserMediaItem(
    val id: Long,
    val userId: String,
    val mediaUrl: String, // S3 URL
    val mediaType: MediaType,
    val displayOrder: Int, // 1-6
    val isPrimary: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime
) {
    init {
        require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        require(mediaUrl.isNotBlank()) { "Media URL is required" }
    }
}

/**
 * Request DTO for adding user media
 */
@Serializable
data class AddUserMediaRequest(
    val userId: Long,
    val mediaUrl: String,
    val mediaType: MediaType,
    val displayOrder: Int,
    val isPrimary: Boolean = false
) {
    init {
        require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        require(mediaUrl.isNotBlank()) { "Media URL is required" }
    }
}

/**
 * Request DTO for updating user media
 */
@Serializable
data class UpdateUserMediaRequest(
    val mediaUrl: String? = null,
    val displayOrder: Int? = null,
    val isPrimary: Boolean? = null
) {
    init {
        if (displayOrder != null) {
            require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        }
    }
}

/**
 * Response containing all media for a user, ordered by displayOrder
 */
@Serializable
data class UserMediaCollection(
    val userId: String,
    val media: List<UserMediaItem>,
    val totalCount: Int
) {
    /**
     * Check if user has minimum required media (3)
     */
    fun hasMinimumMedia(): Boolean = totalCount >= 3
    
    /**
     * Check if user has reached maximum media (6)
     */
    fun hasReachedMaximum(): Boolean = totalCount >= 6
    
    /**
     * Get the primary media item
     */
    fun getPrimaryMedia(): UserMediaItem? = media.firstOrNull { it.isPrimary }
    
    /**
     * Check if collection is valid (3-6 items)
     */
    fun isValidCollection(): Boolean = totalCount in 3..6
}
