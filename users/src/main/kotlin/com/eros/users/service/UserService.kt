package com.eros.users.service

import com.eros.users.ProfileCompleteness
import com.eros.users.models.AdminUpdateUserRequest
import com.eros.users.models.Badge
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.ProfileStatus
import com.eros.users.models.Role
import com.eros.users.models.UpdateUserRequest
import com.eros.users.models.User
import com.eros.users.models.ValidationStatus
import com.eros.users.repository.UserRepository
import com.eros.users.table.badgeHelper
import com.google.firebase.auth.FirebaseAuth
import java.time.Clock
import java.time.Instant

/**
 * Service layer for user operations.
 *
 * Responsible for mapping request DTOs to domain entities before delegating to the
 * repository. This keeps the repository free of DTO coupling and ensures business
 * logic (including DTO → domain translation) lives in one place.
 */
class UserService(
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * Creates a new user profile.
     *
     * Maps [CreateUserRequest] to a [User] domain entity and persists it.
     *
     * @param request CreateUserRequest containing all required user profile data
     * @return The created User
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun createUser(request: CreateUserRequest): User {
        val now = Instant.now(clock)
        val user = User(
            userId = request.userId,
            firstName = request.firstName,
            lastName = request.lastName,
            email = request.email,
            heightCm = request.heightCm,
            dateOfBirth = request.dateOfBirth,
            city = request.city,
            educationLevel = request.educationLevel,
            gender = request.gender,
            occupation = request.occupation ?: "",
            bio = request.bio,
            interests = request.interests,
            traits = request.traits,
            preferredLanguage = request.preferredLanguage,
            spokenLanguages = request.spokenLanguages,
            religion = request.religion,
            politicalView = request.politicalView,
            alcoholConsumption = request.alcoholConsumption,
            smokingStatus = request.smokingStatus,
            diet = request.diet,
            dateIntentions = request.dateIntentions,
            relationshipType = request.relationshipType,
            kidsPreference = request.kidsPreference,
            sexualOrientation = request.sexualOrientation,
            pronouns = request.pronouns,
            starSign = request.starSign,
            ethnicity = request.ethnicity,
            brainAttributes = request.brainAttributes,
            brainDescription = request.brainDescription,
            bodyAttributes = request.bodyAttributes,
            bodyDescription = request.bodyDescription,
            createdAt = now,
            updatedAt = now,
            eloScore = 1000,
            photoValidationStatus = ValidationStatus.UNVALIDATED,
            profileStatus = ProfileStatus.ACTIVE,
            badges = null,
            role = Role.USER,
            coordinatesLongitude = request.coordinatesLongitude,
            coordinatesLatitude = request.coordinatesLatitude,
            profileCompleteness = 50
        )
        return userRepository.create(user)
    }

    /**
     * Updates an existing user profile (user-editable fields only).
     *
     * Fetches the current [User] from the repository, merges the non-null fields from
     * [UpdateUserRequest] onto it, then persists the merged entity.
     *
     * Server-managed fields (eloScore, role, profileStatus, badges, etc.) cannot be
     * modified through this method. Use [adminUpdateUser] for admin-level updates.
     *
     * @param userId Firebase UID of the user to update
     * @param request UpdateUserRequest containing fields to update
     * @return The updated User, or null if user not found
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun updateUser(userId: String, request: UpdateUserRequest): User? {
        val existing = userRepository.findById(userId) ?: return null
        val merged = existing.copy(
            firstName = request.firstName ?: existing.firstName,
            lastName = request.lastName ?: existing.lastName,
            email = request.email ?: existing.email,
            heightCm = request.heightCm ?: existing.heightCm,
            city = request.city ?: existing.city,
            educationLevel = request.educationLevel ?: existing.educationLevel,
            occupation = request.occupation ?: existing.occupation,
            bio = request.bio ?: existing.bio,
            interests = request.interests ?: existing.interests,
            traits = request.traits ?: existing.traits,
            preferredLanguage = request.preferredLanguage ?: existing.preferredLanguage,
            spokenLanguages = request.spokenLanguages ?: existing.spokenLanguages,
            religion = request.religion ?: existing.religion,
            politicalView = request.politicalView ?: existing.politicalView,
            alcoholConsumption = request.alcoholConsumption ?: existing.alcoholConsumption,
            smokingStatus = request.smokingStatus ?: existing.smokingStatus,
            diet = request.diet ?: existing.diet,
            dateIntentions = request.dateIntentions ?: existing.dateIntentions,
            relationshipType = request.relationshipType ?: existing.relationshipType,
            kidsPreference = request.kidsPreference ?: existing.kidsPreference,
            sexualOrientation = request.sexualOrientation ?: existing.sexualOrientation,
            pronouns = request.pronouns ?: existing.pronouns,
            starSign = request.starSign ?: existing.starSign,
            ethnicity = request.ethnicity ?: existing.ethnicity,
            brainAttributes = request.brainAttributes ?: existing.brainAttributes,
            brainDescription = request.brainDescription ?: existing.brainDescription,
            bodyAttributes = request.bodyAttributes ?: existing.bodyAttributes,
            bodyDescription = request.bodyDescription ?: existing.bodyDescription,
            coordinatesLongitude = request.coordinatesLongitude ?: existing.coordinatesLongitude,
            coordinatesLatitude = request.coordinatesLatitude ?: existing.coordinatesLatitude
        )
        return userRepository.update(userId, merged)
    }

    /**
     * Updates server-managed fields for a user (admin-only operation).
     *
     * This method should only be called after verifying that the caller has ADMIN or
     * EMPLOYEE role. It allows updating sensitive fields like role, badges, ELO score,
     * and profile status.
     *
     * @param userId Firebase UID of the user to update
     * @param request AdminUpdateUserRequest containing admin-level fields to update
     * @return The updated User, or null if user not found
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun adminUpdateUser(userId: String, request: AdminUpdateUserRequest): User? {
        val existing = userRepository.findById(userId) ?: return null
        val merged = existing.copy(
            eloScore = request.eloScore ?: existing.eloScore,
            photoValidationStatus = request.photoValidationStatus ?: existing.photoValidationStatus,
            profileStatus = request.profileStatus ?: existing.profileStatus,
            badges = when {
                request.verifiedPhotoBadge != null || request.goodExperienceBadge != null || request.trustedBadge != null -> {
                    badgeHelper(
                        (request.verifiedPhotoBadge ?: false) to Badge.VERIFIED,
                        (request.goodExperienceBadge ?: false) to Badge.GOOD_XP,
                        (request.trustedBadge ?: false) to Badge.TRUSTED
                    )
                }
                else -> existing.badges
            },
            role = request.role ?: existing.role,
            profileCompleteness = request.profileCompleteness ?: existing.profileCompleteness
        )

        // If role was updated, sync with Firebase custom claims
        if (request.role != null && request.role != existing.role) {
            val claims = mapOf("role" to merged.role)
            FirebaseAuth.getInstance().setCustomUserClaims(userId, claims)
        }

        return userRepository.update(userId, merged)
    }

    /**
     * Finds a user by Firebase UID.
     *
     * @param userId Firebase user ID to search for
     * @return User if found, null otherwise
     */
    suspend fun findByUserId(userId: String): User? {
        return userRepository.findById(userId)
    }

    /**
     * Finds a user by email address.
     *
     * @param email Email address to search for
     * @return User if found, null otherwise
     */
    suspend fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    /**
     * Deletes a user by Firebase UID (soft delete).
     *
     * Used when a user deletes their account for GDPR compliance.
     *
     * @param userId Firebase UID of the user to delete
     * @return Number of rows updated (1 if successful, 0 if user not found)
     */
    suspend fun deleteUser(userId: String): Int {
        return userRepository.delete(userId)
    }

    /**
     * Checks if a user exists by Firebase UID.
     *
     * @param userId Firebase UID to check
     * @return True if user exists, false otherwise
     */
    suspend fun userExists(userId: String): Boolean {
        return userRepository.doesExist(userId)
    }

    /**
     * Function to return the shared interests of two users.
     */
    fun getSharedInterests(user1: User, user2: User): List<String> {
        if (user1 == user2){return user1.interests}
        return (user1.interests intersect user2.interests.toSet()).toList()
    }

}
