package com.eros.matching.models

import com.eros.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------------------------------------------------------------------------
// Domain Model
// ---------------------------------------------------------------------------

/**
 * Match domain model representing a potential match between two users.
 *
 * Matches are created in daily batches based on [createdAt] date. Users can have a maximum
 * of 30 unserved matches at any time. Once served, users can see up to 15 matches per day
 * based on [servedAt] timestamp.
 *
 * @property matchId Auto-incrementing primary key
 * @property user1Id First user in the match pair (the user who will see this match)
 * @property user2Id Second user in the match pair (the potential match)
 * @property liked Whether user1 liked user2
 * @property createdAt When the match was created (determines batch date)
 * @property updatedAt When the match was last updated (changes when user acts)
 * @property servedAt When the match was served to user1 (null if not yet shown)
 */
data class Match(
    val matchId: Long,
    val user1Id: String,
    val user2Id: String,
    val liked: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val servedAt: Instant? = null
) {
    init {
        require(user1Id != user2Id) { "Cannot create match with same user (user1_id: $user1Id, user2_id: $user2Id)" }
        require(user1Id.isNotBlank()) { "user1Id cannot be blank" }
        require(user2Id.isNotBlank()) { "user2Id cannot be blank" }
        require(createdAt <= updatedAt) { "createdAt must be before or equal to updatedAt" }
        if (servedAt != null) {
            require(createdAt <= servedAt) { "createdAt must be before or equal to servedAt" }
        }
    }

    // -----------------------------------------------------------------------
    // Status helpers
    // -----------------------------------------------------------------------

    /**
     * Check if match has not been served to user1 yet.
     */
    fun isPending(): Boolean = servedAt == null

    /**
     * Check if match has been served to user1.
     */
    fun isServed(): Boolean = servedAt != null

    /**
     * Check if user1 has taken action (like or pass) on this match.
     * User has acted if the match has been served and subsequently updated.
     */
    fun hasUserActed(): Boolean = servedAt != null && updatedAt > servedAt

    /**
     * Check if user1 liked user2.
     */
    fun isLiked(): Boolean = liked

    /**
     * Check if user1 passed on user2 (served but not liked).
     */
    fun isPassed(): Boolean = isServed() && !liked

    // -----------------------------------------------------------------------
    // Time-based helpers
    // -----------------------------------------------------------------------

    /**
     * Get the time elapsed since the match was served to the user.
     * Returns null if the match hasn't been served yet.
     */
    fun getTimeSinceServed(): Duration? {
        return servedAt?.let { Duration.between(it, Instant.now()) }
    }

    /**
     * Get the date this match was created (for batch grouping).
     * Uses UTC timezone.
     */
    fun getCreatedDate(): LocalDate {
        return createdAt.atZone(ZoneId.of("UTC")).toLocalDate()
    }

    /**
     * Get the date this match was served (for daily serving limits).
     * Returns null if not yet served. Uses UTC timezone.
     */
    fun getServedDate(): LocalDate? {
        return servedAt?.atZone(ZoneId.of("UTC"))?.toLocalDate()
    }

    // -----------------------------------------------------------------------
    // Business logic helpers
    // -----------------------------------------------------------------------

    /**
     * Check if this match belongs to the specified date batch.
     * Matches are batched by the date they were created.
     */
    fun isInBatch(date: LocalDate): Boolean {
        return getCreatedDate() == date
    }

    /**
     * Check if this match was served on the specified date.
     */
    fun wasServedOn(date: LocalDate): Boolean {
        return getServedDate() == date
    }

    /**
     * Create a copy of this match with servedAt set to now.
     */
    fun markAsServed(servedAt: Instant = Instant.now()): Match {
        return copy(servedAt = servedAt)
    }

    /**
     * Create a copy of this match with the user's action recorded.
     *
     * @param liked Whether the user liked this match (true) or passed (false)
     */
    fun recordAction(liked: Boolean, timestamp: Instant = Instant.now()): Match {
        return copy(
            liked = liked,
            updatedAt = timestamp
        )
    }
}

// ---------------------------------------------------------------------------
// DTOs
// ---------------------------------------------------------------------------

/**
 * Request DTO for a user taking action on a potential match.
 *
 * Sent when a user likes or passes on a match they've been served.
 *
 * @property matchId The ID of the match being acted upon
 * @property liked Whether the user liked the match (true) or passed (false)
 */
@Serializable
data class MatchActionRequest(
    val matchId: Long,
    val liked: Boolean
) {
    init {
        require(matchId > 0) { "matchId must be positive" }
    }
}

/**
 * DTO returned when both users have liked each other, creating a mutual match.
 *
 * This triggers the "It's a Match!" scenario and enables the dating module.
 *
 * @property matchId The ID of the match record
 * @property user1Id First user in the mutual match
 * @property user2Id Second user in the mutual match
 * @property matchedAt When the mutual match was confirmed
 */
@Serializable
data class MutualMatchInfo(
    val matchId: Long,
    val user1Id: String,
    val user2Id: String,
    @Serializable(with = InstantSerializer::class)
    val matchedAt: Instant
)
