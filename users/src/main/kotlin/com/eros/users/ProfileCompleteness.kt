package com.eros.users

import com.eros.users.models.User
import com.eros.users.models.UserMediaCollection
import com.eros.users.models.UserQACollection

// Photos (35)
private const val SCORE_PHOTO_3 = 20
private const val SCORE_PHOTO_4 = 32
private const val SCORE_PHOTO_5_PLUS = 35

// Core Identity (35)
private const val SCORE_BIO = 10
private const val SCORE_OCCUPATION = 5
private const val SCORE_TRAITS = 10
private const val SCORE_INTERESTS = 10

// Lifestyle & Values (5)
private const val SCORE_RELIGION = 1
private const val SCORE_POLITICAL_VIEW = 1
private const val SCORE_ALCOHOL = 1
private const val SCORE_SMOKING = 1
private const val SCORE_DIET = 1

// User QA (10)
private const val SCORE_QA_1 = 3
private const val SCORE_QA_2 = 6
private const val SCORE_QA_3_PLUS = 10


private const val MAX_SCORE = SCORE_PHOTO_5_PLUS+SCORE_BIO+SCORE_OCCUPATION+SCORE_TRAITS+SCORE_INTERESTS+
        +SCORE_RELIGION+SCORE_POLITICAL_VIEW+ SCORE_ALCOHOL+SCORE_SMOKING+SCORE_DIET + SCORE_QA_3_PLUS
private const val MIN_PROFILE_COMPLETENESS = 50


/**
 * Breakdown of the completeness score by category.
 * Useful for surfacing "what to fill in next" prompts to the user.
 */
data class CompletenessBreakdown(
    val photoScore: Int,
    val coreIdentityScore: Int,
    val lifestyleScore: Int,
    val totalScore: Int,
    val userQAScore : Int,
    val isMatchingEligible: Boolean
)


class ProfileCompleteness {

    /**
     * Algorithm to calculate a user's profile completeness rating.
     *
     * Scoring breakdown (max 100):
     *   - Photos:              35 pts  (scales with photo count)
     *   - Core identity:       35 pts  (bio, occupation, traits, interests)
     *   - Relationship intent: 15 pts  (dateIntentions, relationshipType, kidsPreference)
     *   - User QA:             10 pts  (number of QA's)
     *   - Lifestyle & values:  5 pts   (religion, politics, alcohol, smoking, diet)
     *
     * @param user The user's profile domain model
     * @param userMedia The user's media collection
     * @return Integer between 0–100 representing completeness (100 = fully complete)
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
    fun getBreakdown(user: User, userMedia: UserMediaCollection, userQA : UserQACollection): CompletenessBreakdown {

        val photoScore = calculatePhotoScore(userMedia)
        val coreIdentityScore = calculateCoreIdentityScore(user)
        val lifestyleScore = calculateLifestyleScore(user)
        val userQAScore = calculateQAScore(userQA)

        val totalScore = (photoScore + coreIdentityScore + lifestyleScore + userQAScore)

        return CompletenessBreakdown(
            photoScore = photoScore,
            coreIdentityScore = coreIdentityScore,
            lifestyleScore = lifestyleScore,
            totalScore = totalScore,
            userQAScore = userQAScore,
            isMatchingEligible = totalScore >= MIN_PROFILE_COMPLETENESS
        )
    }


    /**
     * Function for determining if a profile has sufficient information to be eligible for matching.
     * Requires minimum completeness score of [MIN_PROFILE_COMPLETENESS]
     *
     * @param user The user's profile domain model
     * @param userMedia The user's media collection
     * @return `true` if the profile meets the matching threshold, otherwise `false`
     */
    fun isMatchingEligible(user: User, userMedia: UserMediaCollection, userQA : UserQACollection): Boolean {
        return getBreakdown(user, userMedia, userQA).isMatchingEligible
    }


    /**
     * Photos: 35 points.
     */
    private fun calculatePhotoScore(userMedia: UserMediaCollection): Int {
        return when (userMedia.totalCount) {
            0 -> 0
            1 -> 0
            2 -> 0
            3 -> SCORE_PHOTO_3
            4 -> SCORE_PHOTO_4
            else -> SCORE_PHOTO_5_PLUS
        }
    }


    /**
     * Core identity: 35 points.
     */
    private fun calculateCoreIdentityScore(user: User): Int {
        var score = 0

        if (user.bio.isNotBlank() && user.hasValidBio()) score += SCORE_BIO
        if (user.occupation.isNotBlank()) score += SCORE_OCCUPATION
        if (user.hasValidTraitsCount()) score += SCORE_TRAITS
        if (user.hasValidInterestsCount()) score += SCORE_INTERESTS

        return score
    }


    /**
     * UserQA : 10 Points
     */
    private fun calculateQAScore(userQA : UserQACollection) : Int{
        return when (userQA.totalCount) {
            0 -> 0
            1 -> SCORE_QA_1
            2 -> SCORE_QA_2
            else -> SCORE_QA_3_PLUS
        }
    }


    /**
     * Lifestyle & values: 5 points.
     */
    private fun calculateLifestyleScore(user: User): Int {
        var score = 0

        if (user.religion.field != null) score += SCORE_RELIGION
        if (user.politicalView.field != null) score += SCORE_POLITICAL_VIEW
        if (user.alcoholConsumption.field != null) score += SCORE_ALCOHOL
        if (user.smokingStatus.field != null) score += SCORE_SMOKING
        if (user.diet.field != null) score += SCORE_DIET

        return score
    }
}