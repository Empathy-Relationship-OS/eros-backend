package com.eros.users.repository

import com.eros.users.models.MediaType
import com.eros.users.models.UserMediaItem

/**
 * Repository for the [com.eros.users.table.UserMedia] table.
 *
 * The table has a composite ownership pattern (each row belongs to a user,
 * keyed by [userId] + [displayOrder]) that does not map cleanly to the generic
 * [com.eros.database.repository.IBaseDAO]. All operations are defined here directly.
 */
interface PhotoRepository {

    /**
     * Returns all media items for [userId], ordered by display_order ascending.
     */
    suspend fun findByUserId(userId: String): List<UserMediaItem>

    /**
     * Returns a single media item by its auto-generated [id].
     */
    suspend fun findById(id: Long): UserMediaItem?

    /**
     * Returns the media item occupying [displayOrder] for [userId], or null.
     */
    suspend fun findByDisplayOrder(userId: String, displayOrder: Int): UserMediaItem?

    /**
     * Inserts a new media record.
     *
     * @param userId       Firebase UID of the owner.
     * @param mediaUrl     Canonical public URL for the original file.
     * @param mediaType    e.g. "PHOTO"
     * @param displayOrder 1-6
     * @param isPrimary    Whether this is the user's primary photo.
     * @return The inserted [UserMediaItem].
     */
    suspend fun insert(
        userId: String,
        mediaUrl: String,
        mediaType: MediaType,
        displayOrder: Int,
        isPrimary: Boolean
    ): UserMediaItem

    /**
     * Updates the [thumbnailUrl] for an existing media item.
     * Called once Lambda has finished generating the thumbnail.
     */
    suspend fun updateThumbnailUrl(id: Long, thumbnailUrl: String): UserMediaItem?

    /**
     * Sets [isPrimary] = true for [id] and false for all other rows of the same user.
     * Executed in a single transaction to maintain the partial-unique constraint.
     *
     * @return The updated item, or null if [id] does not exist.
     */
    suspend fun setPrimary(userId: String, id: Long): UserMediaItem?

    /**
     * Hard-deletes the media item with [id].
     *
     * @return Number of rows deleted (0 or 1).
     */
    suspend fun deleteById(id: Long): Int

    /**
     * Counts how many media items [userId] currently has.
     */
    suspend fun countByUserId(userId: String): Int
}
