package com.eros.wallet.models

import kotlinx.serialization.Serializable

/**
 * Request DTO for purchasing tokens.
 *
 * @property packageType The token package to purchase (e.g., "STARTER", "POPULAR", "PREMIUM")
 * @property paymentMethodId Stripe payment method ID from client
 * @property idempotencyKey Client-generated unique key to prevent duplicate purchases
 */
@Serializable
data class PurchaseRequest(
    val packageType: String,
    val paymentMethodId: String,
    val idempotencyKey: String
) {
    init {
        require(packageType.isNotBlank()) { "Package type cannot be blank" }
        require(paymentMethodId.isNotBlank()) { "Payment method ID cannot be blank" }
        require(idempotencyKey.isNotBlank()) { "Idempotency key cannot be blank" }
    }
}


/**
 * Response DTO for purchase intent creation.
 *
 * @property clientSecret Stripe client secret for confirming payment on frontend
 * @property paymentIntentId Stripe payment intent ID
 * @property amount Amount in the smallest currency unit (pence for GBP, cents for USD)
 * @property currency Currency code (e.g., "gbp", "usd")
 * @property tokenAmount Number of tokens being purchased
 * @property status Payment intent status (e.g., "requires_confirmation", "succeeded")
 * @property newBalance Optional - wallet balance if payment succeeded immediately
 * @property transactionId Optional - transaction ID if payment succeeded immediately
 */
@Serializable
data class PurchaseResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val amount: Long,
    val currency: String,
    val tokenAmount: Double,
    val status: String,
    val newBalance: Double? = null,
    val transactionId: Long? = null
)