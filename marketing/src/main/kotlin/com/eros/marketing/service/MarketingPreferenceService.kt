package com.eros.marketing.service

import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.NotFoundException
import com.eros.marketing.models.UserMarketingConsent
import com.eros.marketing.repository.MarketingRepository
import java.time.Clock
import java.time.Instant

/**
 * Service layer for marketing preference business logic.
 *
 * Handles CRUD operations for user marketing consent records.
 */
class MarketingPreferenceService(
    private val marketingRepository: MarketingRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * Gets a user's marketing consent record.
     *
     * Business rules:
     * - Returns existing record if found
     * - Returns default record (marketingConsent = false) if user has no record
     *
     * @param userId The user's unique identifier
     * @return UserMarketingConsent record (either existing or default)
     */
    suspend fun getMarketingPreference(userId: String): UserMarketingConsent {
        return marketingRepository.findById(userId) ?: createDefaultConsent(userId)
    }

    /**
     * Creates a new marketing consent record for a user.
     *
     * Business rules:
     * - User can only create their own consent record
     * - If record already exists, this will fail (use update instead)
     *
     * @param userId The user's unique identifier
     * @param requestingUserId The user making the request (for authorization)
     * @param marketingConsent Whether the user consents to marketing communications
     * @return Created UserMarketingConsent record
     * @throws ForbiddenException if requestingUserId doesn't match userId
     */
    suspend fun createMarketingPreference(
        userId: String,
        requestingUserId: String,
        marketingConsent: Boolean
    ): UserMarketingConsent {
        // Verify user can only create their own preference
        if (userId != requestingUserId) {
            throw ForbiddenException("You can only create your own marketing preferences")
        }

        val now = Instant.now(clock)
        val consent = UserMarketingConsent(
            userId = userId,
            marketingConsent = marketingConsent,
            createdAt = now,
            updatedAt = now
        )

        return marketingRepository.create(consent)
    }

    /**
     * Updates an existing marketing consent record.
     *
     * Business rules:
     * - User can only update their own consent record
     * - Creates new record if none exists (upsert behavior)
     *
     * @param userId The user's unique identifier
     * @param requestingUserId The user making the request (for authorization)
     * @param marketingConsent The new marketing consent value
     * @return Updated UserMarketingConsent record
     * @throws ForbiddenException if requestingUserId doesn't match userId
     */
    suspend fun updateMarketingPreference(
        userId: String,
        requestingUserId: String,
        marketingConsent: Boolean
    ): UserMarketingConsent {
        // Verify user can only update their own preference
        if (userId != requestingUserId) {
            throw ForbiddenException("You can only update your own marketing preferences")
        }

        val existing = marketingRepository.findById(userId)

        return if (existing != null) {
            // Update existing record
            val updated = existing.updateConsent(marketingConsent, Instant.now(clock))
            marketingRepository.update(userId, updated)
                ?: throw NotFoundException("Marketing preference for user $userId was deleted during update")
        } else {
            // Create new record if none exists (upsert behavior)
            val now = Instant.now(clock)
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = marketingConsent,
                createdAt = now,
                updatedAt = now
            )
            marketingRepository.create(consent)
        }
    }

    /**
     * Deletes a user's marketing consent record.
     *
     * Business rules:
     * - Only admins/employees can delete records
     * - Deletion is idempotent (no error if record doesn't exist)
     *
     * @param userId The user's unique identifier
     * @return True if record was deleted, false if it didn't exist
     */
    suspend fun deleteMarketingPreference(userId: String): Boolean {
        return marketingRepository.delete(userId) > 0
    }

    /**
     * Gets all users who have consented to marketing communications.
     *
     * Business rules:
     * - Only accessible to admins/employees (enforced at route level)
     *
     * @return List of UserMarketingConsent records where marketingConsent is true
     */
    suspend fun getAllConsentedUsers(): List<UserMarketingConsent> {
        return marketingRepository.findAllConsented()
    }

    /**
     * Creates a default marketing consent record for a user who has no record.
     *
     * @param userId The user's unique identifier
     * @return Default UserMarketingConsent (marketingConsent = false)
     */
    private fun createDefaultConsent(userId: String): UserMarketingConsent {
        val now = Instant.now(clock)
        return UserMarketingConsent(
            userId = userId,
            marketingConsent = false,
            createdAt = now,
            updatedAt = now
        )
    }
}
