package com.eros.marketing.service

import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.database.dbQuery
import com.eros.marketing.models.UserMarketingConsent
import com.eros.marketing.repository.MarketingRepository
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(MarketingPreferenceService::class.java)

    /**
     * Finds a user's marketing consent record.
     *
     * Business rules:
     * - Returns existing record if found
     * - Returns null if user has no record (does not fabricate default)
     *
     * Used by admin endpoints to distinguish between users with no consent record.
     *
     * @param userId The user's unique identifier
     * @return UserMarketingConsent record if found, null otherwise
     */
    suspend fun findMarketingPreference(userId: String): UserMarketingConsent? = dbQuery {
        marketingRepository.findById(userId)
    }

    /**
     * Gets a user's marketing consent record.
     *
     * Business rules:
     * - Returns existing record if found
     * - Creates and persists default record (marketingConsent = false) if user has no record
     * - Logs when default is created (should be rare since consent is required during signup)
     *
     * @param userId The user's unique identifier
     * @return UserMarketingConsent record (either existing or newly created default)
     */
    suspend fun getMarketingPreference(userId: String): UserMarketingConsent = dbQuery {
        marketingRepository.findById(userId) ?: createAndPersistDefaultConsent(userId)
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
     * @throws ConflictException if record already exists for the user
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

        return dbQuery {
            // Check if record already exists
            if (marketingRepository.doesExist(userId)) {
                throw ConflictException("Marketing preference already exists for user $userId. Use PUT to update.")
            }

            val now = Instant.now(clock)
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = marketingConsent,
                createdAt = now,
                updatedAt = now
            )

            try {
                marketingRepository.create(consent)
            } catch (e: ExposedSQLException) {
                // Handle race condition where another request created the record after doesExist check
                // PostgreSQL unique constraint violation error code is 23505
                if (e.sqlState == "23505") {
                    throw ConflictException("Marketing preference already exists for user $userId. Use PUT to update.")
                }
                throw e
            }
        }
    }

    /**
     * Updates an existing marketing consent record.
     *
     * Business rules:
     * - User can only update their own consent record
     * - Creates new record if none exists (upsert behavior)
     * - Uses atomic database upsert to prevent race conditions
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

        return dbQuery {
            val now = Instant.now(clock)
            val consent = UserMarketingConsent(
                userId = userId,
                marketingConsent = marketingConsent,
                createdAt = now,
                updatedAt = now
            )

            // Use atomic upsert to prevent race conditions
            marketingRepository.upsert(consent)
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
    suspend fun deleteMarketingPreference(userId: String): Boolean = dbQuery {
        marketingRepository.delete(userId) > 0
    }

    /**
     * Gets all users who have consented to marketing communications.
     *
     * Business rules:
     * - Only accessible to admins/employees (enforced at route level)
     * - Supports pagination to prevent loading all records into memory
     *
     * @param limit Maximum number of records to return (optional, defaults to 100)
     * @param offset Number of records to skip (optional, defaults to 0)
     * @return List of UserMarketingConsent records where marketingConsent is true
     */
    suspend fun getAllConsentedUsers(limit: Int = 100, offset: Long = 0): List<UserMarketingConsent> = dbQuery {
        marketingRepository.findAllConsented(limit, offset)
    }

    /**
     * Gets the total count of users who have consented to marketing communications.
     *
     * Business rules:
     * - Only accessible to admins/employees (enforced at route level)
     * - Useful for pagination metadata
     *
     * @return Count of users with marketingConsent = true
     */
    suspend fun getConsentedUsersCount(): Long = dbQuery {
        marketingRepository.countConsented()
    }

    /**
     * Creates and persists a default marketing consent record for a user who has no record.
     *
     * This should rarely happen since users are required to set their marketing preference
     * during signup. When it does occur, it indicates either:
     * - Legacy user from before this feature existed
     * - Data integrity issue (record was deleted)
     * - Edge case where signup process didn't complete properly
     *
     * We log these occurrences for monitoring and alerting purposes.
     *
     * @param userId The user's unique identifier
     * @return Default UserMarketingConsent (marketingConsent = false), persisted to database
     */
    private suspend fun createAndPersistDefaultConsent(userId: String): UserMarketingConsent {
        logger.warn(
            "Marketing consent record missing for user {}. Creating default (consent=false). " +
            "This should be rare - investigate if happening frequently.",
            userId
        )

        val now = Instant.now(clock)
        val defaultConsent = UserMarketingConsent(
            userId = userId,
            marketingConsent = false,
            createdAt = now,
            updatedAt = now
        )

        // Use upsert to handle race condition where another request might have created it
        return marketingRepository.upsert(defaultConsent)
    }
}
