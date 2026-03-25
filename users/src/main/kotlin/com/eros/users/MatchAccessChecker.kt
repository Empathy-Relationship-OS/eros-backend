package com.eros.users

/**
 * Interface for checking if a user has access to view another user's profile
 * based on matching status and batch serving.
 *
 * This interface abstracts the matching logic to avoid circular dependencies
 * between users and matching modules.
 */
interface MatchAccessChecker {
    /**
     * Checks if a user has been served a match with the target user.
     *
     * @param userId The user who may have been served the match
     * @param targetUserId The potential match user
     * @return True if a served match exists, false otherwise
     */
    suspend fun hasServedMatch(userId: String, targetUserId: String): Boolean

    /**
     * Checks if the target user is in the requesting user's current batch.
     *
     * @param userId The user whose batch to check
     * @param targetUserId The user to look for in the batch
     * @return True if target user is in today's served matches, false otherwise
     */
    suspend fun isInCurrentBatch(userId: String, targetUserId: String): Boolean
}
