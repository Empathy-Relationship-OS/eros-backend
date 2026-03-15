package com.eros.wallet.stripe

import com.stripe.exception.StripeException
import com.stripe.model.PaymentIntent
import com.stripe.param.PaymentIntentCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class StripePaymentService {

    /**
     * Creates a Stripe payment intent for token purchase.
     *
     * @param amountGBP Amount in GBP (e.g., 45.00 for £45)
     * @param userId User ID for metadata
     * @param tokenAmount Number of tokens being purchased
     * @param idempotencyKey Unique key to prevent duplicate charges
     * @return Stripe PaymentIntent
     */
    suspend fun createPaymentIntent(
        amountGBP: BigDecimal,
        userId: String,
        tokenAmount: BigDecimal,
        packageType: String,
        idempotencyKey: String
    ): PaymentIntent = withContext(Dispatchers.IO) {
        try {
            val params = PaymentIntentCreateParams.builder()
                .setAmount((amountGBP * BigDecimal(100)).toLong())  // Convert to pence
                .setCurrency("gbp")
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .putMetadata("userId", userId)
                .putMetadata("tokenAmount", tokenAmount.toString())
                .putMetadata("packageType", packageType)
                .putMetadata("idempotencyKey", idempotencyKey)
                .setDescription("Purchase $tokenAmount tokens ($packageType package)")
                .build()

            // Stripe SDK handles idempotency via request options
            val requestOptions = com.stripe.net.RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build()

            PaymentIntent.create(params, requestOptions)

        } catch (e: StripeException) {
            throw StripePaymentException("Failed to create payment intent: ${e.message}", e)
        }
    }

    /**
     * Retrieves a payment intent by ID.
     */
    suspend fun getPaymentIntent(paymentIntentId: String): PaymentIntent = withContext(Dispatchers.IO) {
        try {
            PaymentIntent.retrieve(paymentIntentId)
        } catch (e: StripeException) {
            throw StripePaymentException("Failed to retrieve payment intent: ${e.message}", e)
        }
    }

    /**
     * Confirms a payment intent (if needed for server-side confirmation).
     */
    suspend fun confirmPaymentIntent(paymentIntentId: String): PaymentIntent = withContext(Dispatchers.IO) {
        try {
            val intent = PaymentIntent.retrieve(paymentIntentId)
            intent.confirm()
        } catch (e: StripeException) {
            throw StripePaymentException("Failed to confirm payment intent: ${e.message}", e)
        }
    }
}

class StripePaymentException(message: String, cause: Throwable? = null) : Exception(message, cause)