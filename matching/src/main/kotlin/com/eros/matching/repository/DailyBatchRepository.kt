package com.eros.matching.repository

import com.eros.matching.models.DailyBatch
import java.time.LocalDate

/**
 * Repository interface for DailyBatch entities.
 *
 * Tracks daily batch counts per user for rate limiting.
 * Composite key: (userId, batchDate)
 */
interface DailyBatchRepository {

    /**
     * Finds the daily batch record for a user on a specific date.
     *
     * @param userId The user's unique identifier
     * @param date The date to check (UTC)
     * @return The daily batch record if found, null otherwise
     */
    suspend fun findByUserAndDate(userId: String, date: LocalDate): DailyBatch?

    /**
     * Creates a new daily batch record for a user.
     *
     * @param dailyBatch The daily batch record to create
     * @return The created daily batch record
     */
    suspend fun create(dailyBatch: DailyBatch): DailyBatch

    /**
     * Updates an existing daily batch record.
     *
     * @param dailyBatch The daily batch record to update
     * @return The updated daily batch record if found, null otherwise
     */
    suspend fun update(dailyBatch: DailyBatch): DailyBatch?

    /**
     * Increments the batch count for a user on a specific date.
     * Creates a new record if one doesn't exist.
     *
     * @param userId The user's unique identifier
     * @param date The date to increment (UTC)
     * @return The updated daily batch record
     */
    suspend fun incrementBatchCount(userId: String, date: LocalDate): DailyBatch

    /**
     * Gets the current batch count for a user on a specific date.
     *
     * @param userId The user's unique identifier
     * @param date The date to check (UTC)
     * @return The batch count (0 if no record exists)
     */
    suspend fun getBatchCount(userId: String, date: LocalDate): Int
}
