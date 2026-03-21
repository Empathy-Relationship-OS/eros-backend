package com.eros.matching.service

import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.matching.models.DailyBatchResponse
import com.eros.matching.models.Match
import com.eros.matching.models.MutualMatchInfo
import com.eros.matching.models.UserMatchProfile
import com.eros.matching.repository.DailyBatchRepository
import com.eros.matching.repository.MatchRepository
import com.eros.matching.transaction.TransactionManager
import com.eros.users.service.UserService
import java.time.Clock
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
    private val transactionManager: TransactionManager,
    private val clock: Clock = Clock.systemUTC()
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
     * - User cannot change like→pass (prevents unmatching)
     * - User can change pass→like only within 24 hours of servedAt
     * - After 24 hours, pass decisions become permanent
     * - Updates match with liked value and current timestamp
     *
     * @param matchId The ID of the match to update
     * @param userId The user taking the action (for validation)
     * @param like Whether the user liked (true) or passed (false)
     * @return Updated match record
     * @throws NotFoundException if match doesn't exist
     * @throws ConflictException if user already took action or trying to change outside 24-hour window
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
        if (existingMatch.hasUserActed()) {
            // Prevent like→pass (cannot unmatch)
            if (existingMatch.isLiked() && !like) {
                throw ConflictException("Cannot change from like to pass. You have already liked this profile.")
            }

            // Prevent pass→like after 24 hours (pass becomes permanent)
            if (!existingMatch.isLiked() && like) {
                val servedAt = existingMatch.servedAt
                    ?: throw ConflictException("Cannot change action on unserved match")

                val twentyFourHoursAgo = Instant.now(clock).minusSeconds(24 * 60 * 60)
                if (servedAt.isBefore(twentyFourHoursAgo)) {
                    throw ConflictException("Cannot change pass to like. The 24-hour reconsideration window has expired.")
                }
            }

            // Prevent changing action if already set to the same value
            if (existingMatch.isLiked() == like) {
                throw ConflictException("You have already ${if (like) "liked" else "passed on"} this profile.")
            }
        }

        // Update match with action
        val updatedMatch = existingMatch.recordAction(like, Instant.now(clock))
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
                matchedAt = Instant.now(clock),
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
     * @return DailyBatchResponse with profiles and batch metadata
     * @throws DailyBatchLimitExceededException if user has reached 3 batches today
     * @throws NoMatchesAvailableException if no unserved matches exist
     */
    suspend fun fetchDailyBatch(userId: String): DailyBatchResponse = transactionManager.execute {
        val today = LocalDate.now(clock)

        // Check daily batch limit
        val batchCount = dailyBatchRepository.getBatchCount(userId, today)
        if (batchCount >= MAX_DAILY_BATCHES) {
            throw DailyBatchLimitExceededException(
                userId = userId,
                batchesUsed = batchCount,
                maxBatches = MAX_DAILY_BATCHES,
                resetAt = calculateMidnightUtc(today.plusDays(1))
            )
        }

        // Fetch unserved matches
        val unservedMatches = matchRepository.findUnservedMatches(userId, BATCH_SIZE)
        if (unservedMatches.isEmpty()) {
            throw NoMatchesAvailableException("No unserved matches available for user $userId")
        }

        // Mark matches as served
        val servedAt = Instant.now(clock)
        val matchIds = unservedMatches.map { it.matchId }
        matchRepository.markAsServed(matchIds, servedAt)

        // Increment batch count
        dailyBatchRepository.incrementBatchCount(userId, today)

        // Build lightweight profile responses
        val profiles = unservedMatches.mapNotNull { match ->
            buildUserMatchProfile(match, servedAt)
        }

        // Calculate batch metadata
        val batchNumber = batchCount + 1
        val remainingBatches = MAX_DAILY_BATCHES - batchNumber

        DailyBatchResponse(
            profiles = profiles,
            batchNumber = batchNumber,
            remainingBatches = remainingBatches
        )
    }

    /**
     * Calculates midnight UTC for a given date.
     *
     * @param date The date to calculate midnight for
     * @return Instant representing midnight UTC on the given date
     */
    private fun calculateMidnightUtc(date: LocalDate): Instant {
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant()
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

    /**
     * Fetches all profiles that the user passed on in the last 24 hours.
     *
     * This allows users to reconsider and potentially like profiles they accidentally passed.
     * The 24-hour window is calculated from the servedAt timestamp.
     *
     * Business rules:
     * - Only matches where user explicitly passed (liked = false)
     * - Only matches served within the last 24 hours
     * - Returns UserMatchProfile including matchId to enable re-action
     * - Returns empty list if no passes in last 24 hours
     *
     * @param userId The user whose passes to retrieve
     * @return List of UserMatchProfile for profiles the user passed on
     */
    suspend fun getPassesInLast24Hours(userId: String): List<UserMatchProfile> = transactionManager.execute {
        val passedMatches = matchRepository.findPassesInLast24Hours(userId)

        // Build lightweight profile responses
        passedMatches.mapNotNull { match ->
            val servedAt = match.servedAt ?: return@mapNotNull null
            buildUserMatchProfile(match, servedAt)
        }
    }
}

/**
 * Exception thrown when user has reached daily batch limit (3 batches).
 * Should be mapped to 429 Too Many Requests in the route layer.
 *
 * @property userId The user who exceeded the limit
 * @property batchesUsed Number of batches already used today
 * @property maxBatches Maximum allowed batches per day
 * @property resetAt When the limit resets (midnight UTC next day)
 */
class DailyBatchLimitExceededException(
    val userId: String,
    val batchesUsed: Int,
    val maxBatches: Int,
    val resetAt: Instant
) : Exception("Daily batch limit of $maxBatches exceeded for user $userId") {
    // TODO: Consider making the error message generic (without userId) for privacy/security
}

/**
 * Exception thrown when no unserved matches are available.
 * Should be mapped to 204 No Content in the route layer.
 */
class NoMatchesAvailableException(message: String) : Exception(message)