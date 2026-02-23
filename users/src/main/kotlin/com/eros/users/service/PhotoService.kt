package com.eros.users.service

import com.eros.common.config.S3Config
import com.eros.users.models.*
import com.eros.users.repository.PhotoRepository
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.*

/**
 * Service for user photo management.
 *
 * ## Upload flow (two steps)
 *
 * **Step 1** — [generatePresignedUploadUrl]:
 *   - Validates content type (JPEG, PNG, HEIC) and file size (500 KB – 10 MB).
 *   - Generates a time-limited S3 presigned PUT URL.
 *   - Returns the presigned URL + the S3 object key the client must echo back in step 2.
 *
 * **Step 2** — [confirmUpload]:
 *   - Client posts back the object key after the direct-to-S3 upload completes.
 *   - Backend performs a `HeadObject` check to verify the file exists in S3.
 *   - If the requested [displayOrder] slot is already occupied the old S3 object
 *     (original + thumbnail) is deleted before the new record is inserted.
 *   - Persists the record and returns the [UserMediaItem].
 *
 * ## Other operations
 * - [getUserMedia] — list all media for a user.
 * - [deletePhoto] — delete a photo from S3 and the database.
 * - [setPrimaryPhoto] — mark one photo as the user's profile picture.
 */
class PhotoService(
    private val photoRepository: PhotoRepository,
    private val s3Config: S3Config,
    private val s3Client: S3Client = buildS3Client(s3Config),
    private val s3Presigner: S3Presigner = buildS3Presigner(s3Config)
) {

    companion object {

        private val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/heic",
            "image/heif"
        )

        private const val MIN_FILE_SIZE_BYTES = 500L * 1024          // 500 KB
        private const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024    // 10 MB
        private const val MAX_PHOTOS_PER_USER = 6

        /**
         * Map from MIME type to file extension used when generating S3 object keys.
         */
        private val CONTENT_TYPE_TO_EXTENSION = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg"  to "jpg",
            "image/png"  to "png",
            "image/heic" to "heic",
            "image/heif" to "heic"
        )

        internal fun buildS3Client(config: S3Config): S3Client {
            val builder = S3Client.builder()
                .region(Region.of(config.region))

            if (config.accessKeyId.isNotBlank() && config.secretAccessKey.isNotBlank()) {
                builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)
                    )
                )
            }
            // If credentials are blank we fall back to the AWS default credentials chain
            // (IAM role, environment variables, ~/.aws/credentials, etc.)
            return builder.build()
        }

        internal fun buildS3Presigner(config: S3Config): S3Presigner {
            val builder = S3Presigner.builder()
                .region(Region.of(config.region))

            if (config.accessKeyId.isNotBlank() && config.secretAccessKey.isNotBlank()) {
                builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)
                    )
                )
            }
            return builder.build()
        }
    }

    // -------------------------------------------------------------------------
    // Step 1 — generate presigned upload URL
    // -------------------------------------------------------------------------

    /**
     * Validates file metadata and returns a presigned S3 PUT URL.
     *
     * @throws IllegalArgumentException if content type or file size is invalid.
     * @throws IllegalStateException if the user has already reached the 6-photo limit.
     */
    suspend fun generatePresignedUploadUrl(
        userId: String,
        request: PresignedUploadRequest
    ): PresignedUploadResponse {
        val contentType = request.contentType.lowercase().trim()

        require(contentType in ALLOWED_CONTENT_TYPES) {
            "Unsupported file type '$contentType'. Allowed: JPEG, PNG, HEIC."
        }
        require(request.fileSizeBytes in MIN_FILE_SIZE_BYTES..MAX_FILE_SIZE_BYTES) {
            "File size must be between 500 KB and 10 MB " +
                    "(received ${request.fileSizeBytes} bytes)."
        }

        val currentCount = photoRepository.countByUserId(userId)
        val slotOccupied = photoRepository.findByDisplayOrder(userId, request.displayOrder) != null

        // If the slot is not occupied and user already has max photos, reject
        if (!slotOccupied && currentCount >= MAX_PHOTOS_PER_USER) {
            throw IllegalStateException(
                "User has reached the maximum of $MAX_PHOTOS_PER_USER photos."
            )
        }

        val extension = CONTENT_TYPE_TO_EXTENSION[contentType] ?: "jpg"
        val objectKey = "photos/$userId/${UUID.randomUUID()}.$extension"

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Config.bucketName) // TODO it needs to be raw bucket here
            .key(objectKey)
            .contentType(contentType)
            .contentLength(request.fileSizeBytes)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(s3Config.presignedUrlTtlMinutes))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString()

        return PresignedUploadResponse(
            uploadUrl        = presignedUrl,
            objectKey        = objectKey,
            expiresInMinutes = s3Config.presignedUrlTtlMinutes
        )
    }

    // -------------------------------------------------------------------------
    // Step 2 — confirm upload
    // -------------------------------------------------------------------------
    // TODO this comment once we have transaction on db's resolved
    /**
     * Verify each finding against the current code and only fix it if needed.
     *
     * In `@users/src/main/kotlin/com/eros/users/service/PhotoService.kt` around lines
     * 175 - 209, The current confirmUpload flow deletes the old photo from S3
     * (deleteFromS3) and removes its DB row (photoRepository.deleteById) before
     * inserting the new record (photoRepository.insert), risking data loss if insert
     * fails; change confirmUpload to perform DB work inside a transaction (wrap
     * photoRepository.findByDisplayOrder, deleteById, insert, and setPrimary in a
     * single transactional unit) and defer S3 deletes until after the transaction
     * commits (use a post-commit hook or enqueue the old object's keys for async
     * deletion) so that deleteFromS3 and thumbnail deletion occur only after the DB
     * insert and setPrimary succeed (keep verifyS3ObjectExists and
     * s3Config.publicUrlFor checks before the transaction).
     */
    /**
     * Confirms a completed upload:
     * 1. Verifies the S3 object exists via HeadObject.
     * 2. If the display-order slot is occupied, deletes the old photo from S3.
     * 3. Persists the new media record.
     *
     * @throws IllegalArgumentException if the S3 object does not exist.
     * @throws IllegalStateException if the user has reached the photo limit and the
     *   requested slot is empty (no old photo to replace).
     */
    suspend fun confirmUpload(
        userId: String,
        request: ConfirmUploadRequest
    ): UserMediaItem {
        // Verify ownership: object key must belong to the authenticated user
        require(request.objectKey.startsWith("photos/$userId/")) {
            "Object key must belong to the authenticated user"
        }

        // Verify the file was actually uploaded to S3
        verifyS3ObjectExists(request.objectKey)

        val mediaUrl = s3Config.publicUrlFor(request.objectKey)

        // Check if this display-order slot is already occupied
        photoRepository.findByDisplayOrder(userId, request.displayOrder)?.let {
            deleteFromS3(it.mediaUrl)
            it.thumbnailUrl?.let { url -> deleteFromS3(url) }
            if (photoRepository.deleteById(it.id) == 0) {
                throw IllegalStateException("Photo with id ${it.id} wasn't deleted")
            } else {
                // TODO log success deletion
            }
        }

        val item = photoRepository.insert(
            userId       = userId,
            mediaUrl     = mediaUrl,
            mediaType    = "PHOTO",
            displayOrder = request.displayOrder,
            isPrimary    = request.isPrimary
        )

        // If this is the first photo or explicitly marked primary, set it as primary
        if (request.isPrimary) {
            return photoRepository.setPrimary(userId, item.id) ?: item
        }

        return item
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns all media for [userId] as a [UserMediaCollection], ordered by displayOrder.
     */
    suspend fun getUserMedia(userId: String): UserMediaCollection {
        val items = photoRepository.findByUserId(userId).map {
            UserMediaItemDTO(
                it.id,
                it.mediaUrl,
                it.thumbnailUrl,
                it.mediaType,
                it.displayOrder,
                it.isPrimary
            )
        }
        return UserMediaCollection(
            userId     = userId,
            media      = items,
            totalCount = items.size
        )
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /**
     * Deletes a photo from both S3 and the database.
     *
     * Deletes the original object and, if present, the thumbnail.
     *
     * @return The deleted [UserMediaItem], or null if it was not found / did not belong to [userId].
     */
    suspend fun deletePhoto(userId: String, photoId: Long): UserMediaItem? {
        val item = photoRepository.findById(photoId) ?: return null
        if (item.userId != userId) return null // ownership check

        deleteFromS3(item.mediaUrl) // We either wrap this in a transaction such that they all roll back on failure. OR we add some cleanup script to the s3 to detect orphans
        item.thumbnailUrl?.let { deleteFromS3(it) }

        photoRepository.deleteById(photoId)
        return item
    }

    // -------------------------------------------------------------------------
    // Set primary
    // -------------------------------------------------------------------------

    /**
     * Sets the photo identified by [photoId] as the primary photo for [userId].
     *
     * @return The updated [UserMediaItem], or null if [photoId] not found / not owned by [userId].
     */
    suspend fun setPrimaryPhoto(userId: String, photoId: Long): UserMediaItem? {
        val item = photoRepository.findById(photoId) ?: return null
        if (item.userId != userId) return null
        return photoRepository.setPrimary(userId, photoId)
    }

    // -------------------------------------------------------------------------
    // Thumbnail callback (called when Lambda finishes)
    // -------------------------------------------------------------------------

    /**
     * Records the [thumbnailUrl] once the Lambda has generated the thumbnail.
     *
     * @return The updated [UserMediaItem], or null if [photoId] not found.
     */
    suspend fun updateThumbnail(photoId: Long, thumbnailUrl: String): UserMediaItem? =
        photoRepository.updateThumbnailUrl(photoId, thumbnailUrl)

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the S3 object key from a full URL (handles both S3 and CDN URLs).
     *
     * Examples:
     * - `https://bucket.s3.eu-west-2.amazonaws.com/photos/uid/abc.jpg` → `photos/uid/abc.jpg`
     * - `https://cdn.example.com/photos/uid/abc.jpg` → `photos/uid/abc.jpg`
     */
    private fun urlToObjectKey(url: String): String {
        val cdnBase = s3Config.cdnBaseUrl?.trimEnd('/')
        return when {
            !cdnBase.isNullOrBlank() && url.startsWith(cdnBase) ->
                url.removePrefix("$cdnBase/")
            else -> {
                // Strip scheme + host from S3 URL
                val path = url.substringAfter("://").substringAfter("/")
                path
            }
        }
    }

    private fun deleteFromS3(url: String) {
        val key = urlToObjectKey(url)
        try {
            s3Client.deleteObject { it.bucket(s3Config.bucketName).key(key) }
        } catch (e: Exception) {
            // Log but don't rethrow — a failed S3 delete should not block the DB delete.
            // Orphaned objects can be cleaned up with an S3 lifecycle rule.
            // TODO this needs to be replaced with a proper logger
            println("WARN: Failed to delete S3 object '$key': ${e.message}")
        }
    }

    /**
     * Verifies that [objectKey] exists in S3 using a HeadObject call.
     *
     * @throws IllegalArgumentException if the object does not exist.
     */
    private fun verifyS3ObjectExists(objectKey: String) {
        try {
            val request = HeadObjectRequest.builder()
                .bucket(s3Config.bucketName)
                .key(objectKey)
                .build()
            s3Client.headObject(request)
        } catch (e: NoSuchKeyException) {
            throw IllegalArgumentException(
                "Upload not found in S3. Ensure the file was uploaded before confirming.",
                e
            )
        }
    }
}
