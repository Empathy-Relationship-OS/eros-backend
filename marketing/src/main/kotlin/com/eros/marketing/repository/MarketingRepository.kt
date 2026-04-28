package com.eros.marketing.repository

import com.eros.database.repository.IBaseDAO
import com.eros.marketing.models.UserMarketingConsent

/**
 * Repository interface for UserMarketingConsent entities providing CRUD operations
 * and marketing-specific query methods.
 */
interface MarketingRepository : IBaseDAO<String, UserMarketingConsent> {

    /**
     * Finds all users who have consented to marketing communications.
     *
     * @return List of user marketing consent records where marketingConsent is true
     */
    suspend fun findAllConsented(): List<UserMarketingConsent>

    /**
     * Counts the number of users who have consented to marketing communications.
     *
     * @return Count of users with marketingConsent = true
     */
    suspend fun countConsented(): Long

    /**
     * Atomically inserts or updates a marketing consent record.
     *
     * This operation is atomic at the database level, preventing race conditions
     * that could occur with separate find-then-create/update operations.
     *
     * @param entity The marketing consent entity to upsert
     * @return The upserted marketing consent record
     */
    suspend fun upsert(entity: UserMarketingConsent): UserMarketingConsent
}
