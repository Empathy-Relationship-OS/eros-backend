package com.eros.wallet.stripe

import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.services.WalletService
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import io.ktor.util.logging.error
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val logger = LoggerFactory.getLogger(StripeWebhookHandler::class.java)

class StripeWebhookHandler(
    private val walletService: WalletService
) {

    /**
     * Handles incoming Stripe webhook events.
     *
     * @param payload Raw webhook payload
     * @param signature Stripe-Signature header
     * @return WebhookResult indicating success/failure
     */
    suspend fun handleWebhook(payload: String, signature: String): WebhookResult {
        // 1. Verify signature to prevent spoofing
        val event = try {
            Webhook.constructEvent(payload, signature, StripeConfig.webhookSecret)
        } catch (e: SignatureVerificationException) {
            logger.error(e) //{ "Invalid Stripe webhook signature" }
            throw InvalidWebhookSignatureException("Invalid webhook signature")
        }

        logger.info ( "Processing Stripe webhook: ${event.type}" )

        // 2. Handle event based on type
        return when (event.type) {
            "payment_intent.succeeded" -> handlePaymentSuccess(event)
            "payment_intent.payment_failed" -> handlePaymentFailure(event)
            "payment_intent.canceled" -> handlePaymentCancelled(event)
            else -> {
                logger.warn ( "Unhandled Stripe event type: ${event.type}" )
                WebhookResult.Ignored(event.type)
            }
        }
    }

    private suspend fun handlePaymentSuccess(event: Event): WebhookResult {
        val paymentIntent = deserializePaymentIntent(event)

        val userId = paymentIntent.metadata["userId"]
            ?: return WebhookResult.Error("Missing userId in metadata")

        val tokenAmount = paymentIntent.metadata["tokenAmount"]?.toBigDecimalOrNull()
            ?: return WebhookResult.Error("Missing tokenAmount in metadata")

        val idempotencyKey = paymentIntent.metadata["idempotencyKey"]
            ?: return WebhookResult.Error("Missing idempotencyKey in metadata")

        val transactionId = paymentIntent.metadata["transactionId"]
            ?: return WebhookResult.Error("Missing transactionId in metadata")

        val amountPaidGBP = BigDecimal(paymentIntent.amount) / BigDecimal(100)  // Convert from pence

        return try {

            // Credit the wallet
            val transaction = walletService.creditBalance(
                userId = userId,
                amount = tokenAmount,
                type = TransactionType.PURCHASE,
                description = "Purchased $tokenAmount tokens",
                stripePaymentIntentId = paymentIntent.id,
                amountPaidGBP = amountPaidGBP,
                idempotencyKey = idempotencyKey,
                metadata = mapOf(
                    "stripe_payment_intent" to paymentIntent.id,
                    "package_type" to (paymentIntent.metadata["packageType"] ?: "unknown")
                )
            )

            //todo: Update the transaction to be successful.
            // Update the transaction status
            val transaction2 = walletService.markTransactionSuccessful(
                transaction.transactionId,
                idempotencyKey = idempotencyKey
            )

            logger.info ( "Payment succeeded for user $userId: $tokenAmount tokens credited" )
            WebhookResult.Success(transaction.transactionId)

        } catch (e: Exception) {
            logger.error("Failed to process payment success for user $userId" )
            WebhookResult.Error("Failed to credit wallet: ${e.message}")
        }
    }

    private suspend fun handlePaymentFailure(event: Event): WebhookResult {
        val paymentIntent = deserializePaymentIntent(event)

        val userId = paymentIntent.metadata["userId"] ?: return WebhookResult.Error("Missing userId")
        val errorMessage = paymentIntent.lastPaymentError?.message ?: "Payment failed"

        logger.warn ( "Payment failed for user $userId: $errorMessage" )

        // TODO: Update pending transaction to FAILED status
        // TODO: Send notification to user

        return WebhookResult.Failure(errorMessage)
    }

    private suspend fun handlePaymentCancelled(event: Event): WebhookResult {
        val paymentIntent = deserializePaymentIntent(event)

        val userId = paymentIntent.metadata["userId"] ?: return WebhookResult.Error("Missing userId")

        logger.info ( "Payment cancelled for user $userId" )

        // TODO: Update pending transaction to CANCELLED status

        return WebhookResult.Cancelled
    }

    private fun deserializePaymentIntent(event: Event): PaymentIntent {
        return event.dataObjectDeserializer
            .`object`
            .orElseThrow { IllegalStateException("Failed to deserialize PaymentIntent") } as PaymentIntent
    }
}

sealed class WebhookResult {
    data class Success(val transactionId: Long) : WebhookResult()
    data class Failure(val reason: String) : WebhookResult()
    data class Error(val message: String) : WebhookResult()
    data class Ignored(val eventType: String) : WebhookResult()
    object Cancelled : WebhookResult()
}

class InvalidWebhookSignatureException(message: String) : Exception(message)