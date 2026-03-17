package com.eros.matching.service

import com.eros.database.dbQuery
import com.eros.matching.models.Match
import com.eros.matching.models.MutualMatchInfo
import com.eros.matching.models.UserMatchProfile
import com.eros.matching.repository.DailyBatchRepository
import com.eros.matching.repository.MatchRepository
import com.eros.users.service.UserService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Service layer for matching business logic.
 *
 * Handles match actions, mutual match detection, and daily batch serving.
 */
class MatchService(
    private val matchRepository: MatchRepository,
    private val dailyBatchRepository: DailyBatchRepository,
    private val userService: UserService
) {

    companion object {
        const val BATCH_SIZE = 7
        const val MAX_DAILY_BATCHES = 3
    }

    suspend fun matchAction(matchId: Long, like: Boolean): Match = dbQuery {
        // todo should we pass in the user commiting the action and detect that they are the same or should we only care about that in route layer?
        val existingMatch = matchRepository.findById(matchId) ?: throw IllegalArgumentException("The match does not exist.")

        matchRepository.update(matchId, existingMatch.copy(liked = like))!!
    }

    suspend fun isMutualMatch(fromUserId: String, toUserId: String): Boolean = dbQuery {
        val originalMatch = matchRepository.getLikeMatch(fromUserId, toUserId)?.isLiked() ?: return@dbQuery false
        val secondaryMatch = matchRepository.getLikeMatch(toUserId, fromUserId)?.isLiked() ?: return@dbQuery false

        originalMatch && secondaryMatch
    }

    suspend fun matchUser(matchId : Long, like: Boolean) : MutualMatchInfo? {
        val match = matchAction(matchId, like)
        val mutualMatch = isMutualMatch(match.user1Id, match.user2Id)
        if (mutualMatch) {
            // TODO send back event to say its a match
            // This works for the single user case, but you have a mutual connection, how do we send the event to the other user that they have a date?
            // Do we send it such that on app start up they will poll to get dates and if a date exists then no more actions can be taken
            return MutualMatchInfo(
                matchId = match.matchId,
                user1Id = match.user1Id,
                user2Id = match.user2Id,
                matchedAt = Instant.now(),
            )
        }
        return null
    }

    /**
     * Fetches the next batch of daily matches for a user.
     *
     * Business rules:
     * - Maximum 7 profiles per batch
     * - Maximum 3 batches per user per day (21 total matches)
     * - Only unserved matches are returned
     * - Matches are marked as served with timestamp
     * - Returns 204 No Content if no matches available
     * - Returns 429 Too Many Requests if daily limit reached
     *
     * @param userId The user requesting matches
     * @return List of UserMatchProfile with lightweight profile data
     * @throws DailyBatchLimitExceededException if user has reached 3 batches today
     * @throws NoMatchesAvailableException if no unserved matches exist
     */
    suspend fun fetchDailyBatch(userId: String): List<UserMatchProfile> = dbQuery {
        val today = LocalDate.now(ZoneId.of("UTC"))

        // Check daily batch limit
        val batchCount = dailyBatchRepository.getBatchCount(userId, today)
        if (batchCount >= MAX_DAILY_BATCHES) {
            throw DailyBatchLimitExceededException("Daily batch limit of $MAX_DAILY_BATCHES exceeded for user $userId")
        }

        // Fetch unserved matches
        val unservedMatches = matchRepository.findUnservedMatches(userId, BATCH_SIZE)
        if (unservedMatches.isEmpty()) {
            throw NoMatchesAvailableException("No unserved matches available for user $userId")
        }

        // Mark matches as served
        val servedAt = Instant.now()
        val matchIds = unservedMatches.map { it.matchId }
        matchRepository.markAsServed(matchIds, servedAt)

        // Increment batch count
        dailyBatchRepository.incrementBatchCount(userId, today)

        // Build lightweight profile responses
        unservedMatches.mapNotNull { match ->
            buildUserMatchProfile(match, servedAt)
        }
    }

    /**
     * Builds a lightweight UserMatchProfile from a match and user data.
     *
     * @param match The match record
     * @param servedAt The timestamp when the match was served
     * @return UserMatchProfile or null if user not found
     */
    private suspend fun buildUserMatchProfile(match: Match, servedAt: Instant): UserMatchProfile? {
        val userData = userService.getUserMatchProfileData(match.user2Id) ?: return null

        return UserMatchProfile(
            matchId = match.matchId,
            userId = userData.userId,
            name = userData.name,
            age = userData.age,
            thumbnailUrl = userData.thumbnailUrl,
            badges = userData.badges,
            servedAt = servedAt
        )
    }
}

/**
 * Exception thrown when user has reached daily batch limit (3 batches).
 * Should be mapped to 429 Too Many Requests in the route layer.
 */
class DailyBatchLimitExceededException(message: String) : Exception(message)

/**
 * Exception thrown when no unserved matches are available.
 * Should be mapped to 204 No Content in the route layer.
 */
class NoMatchesAvailableException(message: String) : Exception(message)