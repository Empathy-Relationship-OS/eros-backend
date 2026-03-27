package com.eros.wallet.stripe

import com.eros.wallet.convertToUserCurrency
import com.eros.wallet.models.TokenPackage
import com.stripe.exception.StripeException
import com.stripe.model.PaymentIntent
import com.stripe.model.Refund
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.RefundCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StripeService {

    /**
     * Creates a Stripe payment intent for token purchase.
     *
     * @param userId User ID for metadata
     * @param tokenPackage TokenPackage the user wishes to purchase.
     * @param paymentMethodId ?
     * @return Stripe PaymentIntent
     */
    fun createPaymentIntent(
        userId: String,
        tokenPackage: TokenPackage,
        paymentMethodId: String,
        idempotencyKey: String,
        userCurrency: String
    ): PaymentIntent {
        val amount = convertToUserCurrency(tokenPackage.priceGBP, userCurrency)

        val params = PaymentIntentCreateParams.builder()
            .setAmount(amount.toLong())
            .setCurrency(userCurrency)
            .setPaymentMethod(paymentMethodId)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                    .build()
            )
            .putMetadata("userId", userId)
            .putMetadata("tokenAmount", tokenPackage.tokens.toString())
            .putMetadata("packageType", tokenPackage.name)
            .putMetadata("idempotencyKey", idempotencyKey)
            .setDescription("Purchase ${tokenPackage.tokens} tokens")
            .build()

        return PaymentIntent.create(params)
    }

    /**
     * Create a refund in stripe.
     */
    fun createRefund(
        paymentIntentId: String,
        amount: Long? = null,
        reason: String? = null,
        metadata: Map<String, String> = emptyMap(),
        idempotencyKey: String
    ): Refund {
        val paramsBuilder = RefundCreateParams.builder()
            .setPaymentIntent(paymentIntentId)

        // Partial refund if amount specified
        amount?.let { paramsBuilder.setAmount(it) }

        // Optional refund reason
        reason?.let { paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER) }

        // Add metadata for tracking
        metadata.forEach { (key, value) ->
            paramsBuilder.putMetadata(key, value)
        }
        paramsBuilder.putMetadata("idempotencyKey", idempotencyKey)

        return Refund.create(paramsBuilder.build())
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