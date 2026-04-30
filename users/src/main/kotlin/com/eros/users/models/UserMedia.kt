package com.eros.users.models

import kotlinx.serialization.Serializable
import java.time.Instant

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

/**
 * Allowed content types for user media uploads.
 */
object MediaConstants {
    val ALLOWED_CONTENT_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/heic",
        "image/heif",
        "image/webp"
    )

    const val MIN_FILE_SIZE_BYTES = 100L * 1024          // 100 KB
    const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024    // 10 MB
    const val MIN_PHOTOS_PER_USER = 3
    const val MAX_PHOTOS_PER_USER = 6

    val CONTENT_TYPE_TO_EXTENSION = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg"  to "jpg",
        "image/png"  to "png",
        "image/heic" to "heic",
        "image/heif" to "heic",
        "image/webp" to "webp"
    )
}

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------


/**
 * A single media item belonging to a user.
 *
 * [mediaUrl] — canonical public URL for the original file (S3 or CDN).
 * [thumbnailUrl] — 300x300 thumbnail URL; null until the Lambda has processed the upload.
 */
data class UserMediaItem(
    val id: Long,
    val userId: String,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val mediaType: MediaType,
    val displayOrder: Int,
    val isPrimary: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        require(mediaUrl.isNotBlank()) { "Media URL is required" }
    }
}

// ---------------------------------------------------------------------------
// Request / response DTOs
// ---------------------------------------------------------------------------


/**
 * Request body for step 1 of the upload flow: obtaining a presigned S3 URL.
 *
 * The client supplies file metadata so the backend can:
 * 1. Validate content type and size before issuing a presigned URL.
 * 2. Embed a content-length condition on the presigned URL so S3 rejects
 *    uploads that exceed [fileSizeBytes].
 */
@Serializable
data class PresignedUploadRequest(
    val fileName: String, // We can check file name as metadata, if fileName doesnt match then user has lied to us about file uploaded
    val contentType: String,   // e.g. "image/jpeg", "image/png", "image/heic"
    val fileSizeBytes: Long,   // used for S3 content-length-range condition
    val displayOrder: Int,     // 1-6 — where this photo should appear
    val isPrimary: Boolean = false
) {
    init {
        require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        require(fileName.isNotBlank()) { "File name is required" }
        require(contentType in MediaConstants.ALLOWED_CONTENT_TYPES) {
            "Content type must be one of: ${MediaConstants.ALLOWED_CONTENT_TYPES}"
        }
        require(fileSizeBytes in MediaConstants.MIN_FILE_SIZE_BYTES..MediaConstants.MAX_FILE_SIZE_BYTES) {
            "File size must be between ${MediaConstants.MIN_FILE_SIZE_BYTES} bytes and ${MediaConstants.MAX_FILE_SIZE_BYTES} bytes"
        }
    }
}


/**
 * Response for step 1: the presigned URL and the object key the client must
 * use when confirming the upload in step 2.
 */
@Serializable
data class PresignedUploadResponse(
    val uploadUrl: String,   // presigned S3 PUT URL (expires in N minutes)
    val objectKey: String,   // S3 object key — send this back in ConfirmUploadRequest
    val expiresInMinutes: Long
)


/**
 * Request body for step 2: confirming a completed upload.
 *
 * The backend will perform a HeadObject check on [objectKey] to verify the
 * file was actually uploaded to S3 before persisting the record.
 */
@Serializable
data class ConfirmUploadRequest(
    val objectKey: String,
    val displayOrder: Int,
    val isPrimary: Boolean = false
) {
    init {
        require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        require(objectKey.isNotBlank()) { "Object key is required" }
    }
}

/**
 * Domain object containing all media for a user, ordered by displayOrder.
 */
data class UserMediaCollection (
    val userId: String,
    val media: List<UserMediaItem>,
    val totalCount: Int
) {
    fun hasMinimumMedia(): Boolean = totalCount >= MediaConstants.MIN_PHOTOS_PER_USER
    fun hasReachedMaximum(): Boolean = totalCount >= MediaConstants.MAX_PHOTOS_PER_USER
    fun getPrimaryMedia(): UserMediaItem? = media.firstOrNull { it.isPrimary }
    fun isValidCollection(): Boolean = totalCount in MediaConstants.MIN_PHOTOS_PER_USER..MediaConstants.MAX_PHOTOS_PER_USER
}


/**
 * Response containing all media for a user, ordered by displayOrder.
 */
@Serializable
data class UserMediaCollectionDTO(
    val userId: String,
    val media: List<UserMediaItemDTO>,
    val totalCount: Int
)


/**
 * Function for converting user media collection domain object to a DTO.
 */
fun UserMediaCollection.toDTO() = UserMediaCollectionDTO(
    userId = this.userId,
    media = this.media.map {it.toDTO()},
    totalCount = this.totalCount
)


/**
 * DTO used for mediaItem, extracts details not needed for response
 */
@Serializable
data class UserMediaItemDTO(
    val id: Long,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val mediaType: MediaType,
    val displayOrder: Int,
    val isPrimary: Boolean
)


/**
 * Function for converting the UserMediaDomainItem to a DTO.
 */
fun UserMediaItem.toDTO() = UserMediaItemDTO(
    id = this.id,
    mediaUrl = this.mediaUrl,
    thumbnailUrl = this.thumbnailUrl,
    mediaType = this.mediaType,
    displayOrder = this.displayOrder,
    isPrimary = this.isPrimary
)
