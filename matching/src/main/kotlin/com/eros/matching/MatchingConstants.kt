package com.eros.matching

/**
 * Shared constants for the matching module.
 *
 * Extracted from MatchService to avoid circular dependencies between model and service layers.
 */
object MatchingConstants {
    /**
     * Number of profiles per batch.
     * Each batch contains up to 7 potential matches.
     */
    const val BATCH_SIZE = 7

    /**
     * Maximum number of batches a user can fetch per day.
     * Users can fetch up to 3 batches per day (21 total matches).
     */
    const val MAX_DAILY_BATCHES = 3
}
