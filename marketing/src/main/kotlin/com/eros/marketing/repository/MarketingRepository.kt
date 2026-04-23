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
}
