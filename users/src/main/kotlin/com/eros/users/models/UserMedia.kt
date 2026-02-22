package com.eros.users.models

import com.eros.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

// ---------------------------------------------------------------------------
// Domain model
// ---------------------------------------------------------------------------

/**
 * A single media item belonging to a user.
 *
 * [mediaUrl] — canonical public URL for the original file (S3 or CDN).
 * [thumbnailUrl] — 300x300 thumbnail URL; null until the Lambda has processed the upload.
 */
@Serializable
data class UserMediaItem(
    val id: Long,
    val userId: String,
    val mediaUrl: String,
    val thumbnailUrl: String?,
    val mediaType: MediaType,
    val displayOrder: Int,
    val isPrimary: Boolean,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
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
    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg", "image/png", "image/heic", "image/webp", "image/png"
        )
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
    }
    init {
        require(displayOrder in 1..6) { "Display order must be between 1 and 6" }
        require(fileName.isNotBlank()) { "File name is required" }
        require(contentType in ALLOWED_CONTENT_TYPES) {
            "Content type must be one of: $ALLOWED_CONTENT_TYPES"
        }
        require(fileSizeBytes in 1..MAX_FILE_SIZE_BYTES) {
            "File size must be between 1 byte and $MAX_FILE_SIZE_BYTES bytes"
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
 * Response containing all media for a user, ordered by displayOrder.
 */
@Serializable
data class UserMediaCollection(
    val userId: String,
    val media: List<UserMediaItemDTO>,
    val totalCount: Int
) {
    fun hasMinimumMedia(): Boolean = totalCount >= 3
    fun hasReachedMaximum(): Boolean = totalCount >= 6
    fun getPrimaryMedia(): UserMediaItemDTO? = media.firstOrNull { it.isPrimary }
    fun isValidCollection(): Boolean = totalCount in 3..6
}

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
