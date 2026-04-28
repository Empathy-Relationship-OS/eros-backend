package com.eros.marketing.models

import com.eros.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

// ---------------------------------------------------------------------------
// Domain Model
// ---------------------------------------------------------------------------

/**
 * User marketing consent domain model.
 *
 * Represents a user's preference for receiving marketing communications.
 * Each user has at most one marketing consent record.
 *
 * @property userId The user's unique identifier
 * @property marketingConsent Whether the user has consented to receive marketing communications
 * @property createdAt When the consent record was created
 * @property updatedAt When the consent record was last updated
 */
data class UserMarketingConsent(
    val userId: String,
    val marketingConsent: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(userId.isNotBlank()) { "userId cannot be blank" }
        require(createdAt <= updatedAt) { "createdAt must be before or equal to updatedAt" }
    }

    /**
     * Create a copy of this consent record with updated consent value and timestamp.
     *
     * @param newConsent The new marketing consent value
     * @param timestamp The timestamp of the update (defaults to now)
     */
    fun updateConsent(newConsent: Boolean, timestamp: Instant): UserMarketingConsent {
        return copy(
            marketingConsent = newConsent,
            updatedAt = timestamp
        )
    }
}

// ---------------------------------------------------------------------------
// DTOs
// ---------------------------------------------------------------------------

/**
 * Request DTO for creating a marketing consent record.
 *
 * Used when a user sets their marketing preference for the first time.
 *
 * @property marketingConsent Whether the user consents to receive marketing communications
 */
@Serializable
data class CreateMarketingConsentRequest(
    val marketingConsent: Boolean
)

/**
 * Request DTO for updating a marketing consent record.
 *
 * Used when a user changes their existing marketing preference.
 *
 * @property marketingConsent The new marketing consent value
 */
@Serializable
data class UpdateMarketingConsentRequest(
    val marketingConsent: Boolean
)

/**
 * Response DTO for marketing preference endpoints.
 *
 * Returned when retrieving or modifying a user's marketing consent record.
 *
 * @property userId The user's unique identifier
 * @property marketingConsent Whether the user has consented to receive marketing communications
 * @property createdAt When the consent record was created
 * @property updatedAt When the consent record was last updated
 */
@Serializable
data class MarketingPreferenceResponse(
    val userId: String,
    val marketingConsent: Boolean,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
) {
    companion object {
        /**
         * Convert domain model to response DTO.
         */
        fun fromDomain(consent: UserMarketingConsent): MarketingPreferenceResponse {
            return MarketingPreferenceResponse(
                userId = consent.userId,
                marketingConsent = consent.marketingConsent,
                createdAt = consent.createdAt,
                updatedAt = consent.updatedAt
            )
        }
    }
}
