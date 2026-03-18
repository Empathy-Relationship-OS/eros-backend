package com.eros.matching.repository

import com.eros.database.repository.IBaseDAO
import com.eros.matching.models.Match
import java.time.Instant
import java.time.LocalDate

/**
 * Repository interface for Match entities providing CRUD operations
 * and match-specific query methods.
 */
interface MatchRepository : IBaseDAO<Long, Match> {

    /**
     * Finds a match where user1 has liked user2.
     *
     * @param fromUserId The user who liked (user1)
     * @param toUserId The user who was liked (user2)
     * @return The match if found and liked is true, null otherwise
     */
    suspend fun getLikeMatch(fromUserId: String, toUserId: String): Match?

    /**
     * Finds unserved matches for a user (matches where servedAt is null).
     *
     * @param userId The user to find unserved matches for
     * @param limit Maximum number of matches to return
     * @return List of unserved matches, limited by the specified amount
     */
    suspend fun findUnservedMatches(userId: String, limit: Int): List<Match>

    /**
     * Marks the specified matches as served with the given timestamp.
     *
     * @param matchIds List of match IDs to mark as served
     * @param servedAt The timestamp when matches were served
     * @return Number of matches updated
     */
    suspend fun markAsServed(matchIds: List<Long>, servedAt: Instant): Int

    /**
     * Counts how many matches have been served to a user on a specific date.
     *
     * @param userId The user to count served matches for
     * @param date The date to check (UTC)
     * @return Number of matches served on that date
     */
    suspend fun countServedToday(userId: String, date: LocalDate): Int

    /**
     * Finds a match by the user pair (user1_id, user2_id).
     *
     * @param user1Id First user in the pair
     * @param user2Id Second user in the pair
     * @return The match if found, null otherwise
     */
    suspend fun findByUserPair(user1Id: String, user2Id: String): Match?
}