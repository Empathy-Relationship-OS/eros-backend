package com.eros.matching.service

import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.matching.models.Match
import com.eros.matching.models.MutualMatchInfo
import com.eros.matching.models.UserMatchProfile
import com.eros.matching.repository.DailyBatchRepository
import com.eros.matching.repository.MatchRepository
import com.eros.matching.transaction.TransactionManager
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
    private val userService: UserService,
    private val transactionManager: TransactionManager
) {

    companion object {
        const val BATCH_SIZE = 7
        const val MAX_DAILY_BATCHES = 3
    }

    /**
     * Records a user's action (like/pass) on a match.
     *
     * Business rules:
     * - Match must exist (throws NotFoundException if not found)
     * - User cannot change action once taken (throws ConflictException if liked is not null)
     * - Updates match with liked value and current timestamp
     *
     * @param matchId The ID of the match to update
     * @param userId The user taking the action (for validation)
     * @param like Whether the user liked (true) or passed (false)
     * @return Updated match record
     * @throws NotFoundException if match doesn't exist
     * @throws ConflictException if user already took action on this match
     * @throws ForbiddenException if user doesn't own the match
     */
    suspend fun matchAction(matchId: Long, userId: String, like: Boolean): Match = transactionManager.execute {
        val existingMatch = matchRepository.findById(matchId)
            ?: throw NotFoundException("Match with ID $matchId not found")

        // Verify the user owns this match (is user1)
        if (existingMatch.user1Id != userId) {
            throw ForbiddenException("You do not have permission to act on this match")
        }

        // Check if user has already taken action
        // hasUserActed() returns true when servedAt is set and updatedAt > servedAt
        if (existingMatch.hasUserActed() && existingMatch.isLiked()) {
            throw ConflictException("You have already taken action on this match")
        }

        // Update match with action
        val updatedMatch = existingMatch.recordAction(like)
        matchRepository.update(matchId, updatedMatch)
            ?: throw NotFoundException("Match with ID $matchId was deleted or not found during update")
    }

    suspend fun isMutualMatch(fromUserId: String, toUserId: String): Boolean = transactionManager.execute {
        val originalMatch = matchRepository.getLikeMatch(fromUserId, toUserId)?.isLiked() ?: return@execute false
        val secondaryMatch = matchRepository.getLikeMatch(toUserId, fromUserId)?.isLiked() ?: return@execute false

        originalMatch && secondaryMatch
    }

    /**
     * Handles a user's match action and checks for mutual matches.
     *
     * Business rules:
     * - Records the user's action (like/pass) on the match
     * - If user liked, checks if it's a mutual match (both users liked each other)
     * - Returns MutualMatchInfo if mutual match detected, null otherwise
     *
     * @param matchId The ID of the match being acted upon
     * @param userId The user taking the action
     * @param like Whether the user liked (true) or passed (false)
     * @return MutualMatchInfo if mutual match, null if not
     * @throws NotFoundException if match doesn't exist
     * @throws ConflictException if user already took action
     * @throws ForbiddenException if user doesn't own the match
     */
    suspend fun matchUser(matchId: Long, userId: String, like: Boolean): MutualMatchInfo? {
        val match = matchAction(matchId, userId, like)

        // Only check for mutual match if the user liked (not if they passed)
        if (!like) {
            // TODO: When a user passes, consider showing this profile again in their batch with lower priority
            return null
        }

        val mutualMatch = isMutualMatch(match.user1Id, match.user2Id)
        if (mutualMatch) {
            // TODO: Send event notification to both users about the mutual match
            // TODO: In the matching algorithm, weight liked users more heavily when generating future matches
            // This works for the single user case, but you have a mutual connection, how do we send the event to the other user that they have a date?
            // Do we send it such that on app start up they will poll to get dates and if a date exists then no more actions can be taken
            return MutualMatchInfo(
                matchId = match.matchId,
                user1Id = match.user1Id,
                user2Id = match.user2Id,
                matchedAt = Instant.now(),
            )
        }

        // TODO: When a user likes but no mutual match yet, increase the weight/priority of showing
        // the liking user's profile to the liked user in future batches (matching algorithm enhancement)
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
    suspend fun fetchDailyBatch(userId: String): List<UserMatchProfile> = transactionManager.execute {
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

    /**
     * Checks if a user has been served a match with the target user.
     *
     * This method determines if user1 has been served user2 as a potential match,
     * meaning user2 appeared in one of user1's daily batches.
     *
     * @param userId The user who may have been served the match
     * @param targetUserId The potential match user
     * @return True if a served match exists, false otherwise
     */
    suspend fun hasServedMatch(userId: String, targetUserId: String): Boolean = transactionManager.execute {
        matchRepository.hasServedMatch(userId, targetUserId)
    }

    /**
     * Checks if the target user is in the requesting user's current batch.
     *
     * A "current batch" is defined as a match served today (within the current UTC day).
     *
     * @param userId The user whose batch to check
     * @param targetUserId The user to look for in the batch
     * @return True if target user is in today's served matches, false otherwise
     */
    suspend fun isInCurrentBatch(userId: String, targetUserId: String): Boolean = transactionManager.execute {
        val match = matchRepository.findByUserPair(userId, targetUserId) ?: return@execute false

        // Check if match was served today
        val servedAt = match.servedAt ?: return@execute false
        val today = LocalDate.now(ZoneId.of("UTC"))
        val servedDate = LocalDate.ofInstant(servedAt, ZoneId.of("UTC"))

        servedDate.isAfter(today.minusDays(1))  && servedDate.isBefore(today.plusDays(1))
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