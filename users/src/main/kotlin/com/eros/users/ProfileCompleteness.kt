package com.eros.users

import com.eros.users.models.User
import com.eros.users.models.UserMediaCollection
import com.eros.users.models.UserQACollection

// Photos (20)
private const val SCORE_PHOTOS = 20

// Core Identity (20)
private const val SCORE_BIO = 10
private const val SCORE_TRAITS = 5
private const val SCORE_INTERESTS = 5

// User QA (10)
private const val SCORE_QA = 5

// Minimum score a profile has from required fields.
private const val MIN_PROFILE_SCORE = 50

// Minimum quantity for achieving max score.
private const val TRAITS_QUANTITY = 5
private const val INTERESTS_QUANTITY = 5
private const val PHOTOS_REQUIRED = 4

/**
 * Breakdown of the completeness score by category.
 * Useful for surfacing "what to fill in next" prompts to the user.
 */
data class CompletenessBreakdown(
    val photoScore: Int,
    val coreIdentityScore: Int,
    val totalScore: Int,
    val userQAScore: Int,
)


class ProfileCompleteness {

    /**
     * Algorithm to calculate a user's profile completeness rating.
     *
     * Scoring breakdown (max 50):
     *   - Photos:              20 pts  (scales with photo count)
     *   - Core identity:       20 pts  (bio, occupation, traits, interests)
     *   - User QA:             10 pts  (More than 1 QA)
     *
     * @param user The user's profile domain model
     * @param userMedia The user's media collection
     * @return Integer between 50–100 representing completeness (50 = minimum required, 100 = fully complete)
     */
    fun calculateCompleteness(user: User, userMedia: UserMediaCollection, userQA: UserQACollection): Int {
        return getBreakdown(user, userMedia, userQA).totalScore
    }


    /**
     * Returns a full [CompletenessBreakdown] with per-category scores
     *
     * @param user The user's profile domain model
     * @param userMedia The user's media collection
     */
    fun getBreakdown(user: User, userMedia: UserMediaCollection, userQA: UserQACollection): CompletenessBreakdown {

        val photoScore = calculatePhotoScore(userMedia)
        val coreIdentityScore = calculateCoreIdentityScore(user)
        val userQAScore = calculateQAScore(userQA)

        val totalScore = (photoScore + coreIdentityScore + userQAScore + MIN_PROFILE_SCORE)

        return CompletenessBreakdown(
            photoScore = photoScore,
            coreIdentityScore = coreIdentityScore,
            totalScore = totalScore,
            userQAScore = userQAScore,
        )
    }


    /**
     * Photos: 20 points.
     */
    private fun calculatePhotoScore(userMedia: UserMediaCollection): Int {
        return if (userMedia.totalCount >= PHOTOS_REQUIRED) {
            SCORE_PHOTOS
        } else
            0
    }
}


/**
 * Core identity: 20 points.
 */
private fun calculateCoreIdentityScore(user: User): Int {
    var score = 0

    if (user.bio.isNotBlank() && user.hasValidBio()) score += SCORE_BIO
    if (user.traits.size >= TRAITS_QUANTITY) score += SCORE_TRAITS
    if (user.interests.size >= INTERESTS_QUANTITY) score += SCORE_INTERESTS

    return score
}


/**
 * UserQA : 10 Points
 */
private fun calculateQAScore(userQA: UserQACollection): Int {
    return if (userQA.totalCount > 1) {
        SCORE_QA
    } else {
        0
    }
}