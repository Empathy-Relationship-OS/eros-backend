package com.eros.wallet.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal

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
    val idempotencyKey: String,
    val acceptedTerms: Boolean
) {
    init {
        require(packageType.isNotBlank()) { "Package type cannot be blank" }
        require(paymentMethodId.isNotBlank()) { "Payment method ID cannot be blank" }
        require(idempotencyKey.isNotBlank()) { "Idempotency key cannot be blank" }
        require(acceptedTerms) { "Terms must be accepted to make a purchase" }
    }
}


/**
 * Domain object for a purchase.
 */
data class Purchase(
    val clientSecret: String,
    val paymentIntentId: String,
    val amount: BigDecimal,
    val currency: String,
    val tokenAmount: BigDecimal,
    val status: String,
    val newBalance: BigDecimal? = null,
    val transactionId: Long? = null,
    val acceptedTerms: Boolean
)


/**
 * Response DTO for purchase intent creation.
 *
 * @property clientSecret Stripe client secret for confirming payment on frontend
 * @property paymentIntentId Stripe payment intent ID
 * @property amount Amount in the smallest currency unit (pence for GBP, cents for USD)
 * @property currency Currency code (e.g., "gbp", "usd")
 * @property tokenAmount Number of tokens being purchased
 * @property status Payment intent status (e.g., "PENDING", "COMPLETED")
 * @property newBalance Optional - wallet balance if payment succeeded immediately
 * @property transactionId Optional - transaction ID if payment succeeded immediately
 */
@Serializable
data class PurchaseResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    @Serializable(with = BigDecimalSerializer::class)
    val tokenAmount: BigDecimal,
    val status: String,
    @Serializable(with = BigDecimalSerializer::class)
    val newBalance: BigDecimal? = null,
    val transactionId: Long? = null,
    val acceptedTerms: Boolean?
)

fun Purchase.toDTO() = PurchaseResponse(
    clientSecret = clientSecret,
    paymentIntentId = paymentIntentId,
    amount = amount,
    currency = currency,
    tokenAmount = tokenAmount,
    status = status,
    newBalance = newBalance,
    transactionId = transactionId,
    acceptedTerms = acceptedTerms
)


@Serializable
data class SpendTokenRequest(
    val relatedDateId : Long,
    val activity: String,
    val idempotencyKey : String
)

@Serializable
data class RefundTokenRequest(
    val transactionId: Long,
    val stripePaymentIntent: String,
    val idempotencyKey : String
)


data class Refund(
    val transactionId: Long,
    val clientSecret: String,
    val status: String
)

@Serializable
data class RefundResponse(
    val transactionId: Long,
    val clientSecret: String,
    val status: String
)

fun Refund.toDTO() = RefundResponse(
    transactionId = this.transactionId,
    clientSecret = this.clientSecret,
    status = this.status
)



