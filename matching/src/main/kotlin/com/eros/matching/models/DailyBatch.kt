package com.eros.matching.models

import java.time.Instant
import java.time.LocalDate

/**
 * Domain model representing daily batch tracking for a user.
 *
 * Tracks how many batches a user has fetched on a specific date
 * to enforce the 3-batches-per-day limit.
 *
 * @property userId The user's unique identifier
 * @property batchDate The date (UTC) this record tracks
 * @property batchCount Number of batches fetched on this date (0-3)
 * @property createdAt When the first batch was fetched for this date
 * @property updatedAt When the record was last updated
 */
data class DailyBatch(
    val userId: String,
    val batchDate: LocalDate,
    val batchCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(batchCount in 0..3) { "batchCount must be between 0 and 3, got: $batchCount" }
    }

    /**
     * Check if the user has reached the daily batch limit (3 batches).
     */
    fun hasReachedLimit(): Boolean = batchCount >= 3

    /**
     * Check if the user can fetch another batch.
     */
    fun canFetchBatch(): Boolean = batchCount < 3

    /**
     * Get the number of remaining batches for the day.
     */
    fun remainingBatches(): Int = 3 - batchCount

    /**
     * Create a copy with incremented batch count.
     */
    fun incrementBatchCount(timestamp: Instant = Instant.now()): DailyBatch {
        require(canFetchBatch()) { "Cannot increment batch count: limit reached (current: $batchCount)" }
        return copy(
            batchCount = batchCount + 1,
            updatedAt = timestamp
        )
    }
}
