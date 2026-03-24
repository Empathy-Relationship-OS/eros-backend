package com.eros.wallet.stripe

import com.eros.database.dbQuery
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.services.TransactionService
import com.eros.wallet.services.WalletService
import com.stripe.exception.SignatureVerificationException
import com.stripe.model.Event
import com.stripe.model.PaymentIntent
import com.stripe.net.ApiResource
import com.stripe.net.Webhook
import io.ktor.server.plugins.NotFoundException
import io.ktor.util.logging.error
import org.slf4j.LoggerFactory
import java.math.BigDecimal

private val logger = LoggerFactory.getLogger(StripeWebhookHandler::class.java)

class StripeWebhookHandler(
    private val walletService: WalletService,
    private val transactionService: TransactionService
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
        val dataObject = event.dataObjectDeserializer.rawJson

        // Use ApiResource.GSON to force-map the raw JSON to a PaymentIntent object
        val paymentIntent = ApiResource.GSON.fromJson(dataObject, PaymentIntent::class.java)
            ?: return WebhookResult.Error("Failed to parse raw JSON to PaymentIntent")

        val userId = paymentIntent.metadata["userId"]
            ?: return WebhookResult.Error("Missing userId in metadata")

        val tokenAmount = paymentIntent.metadata["tokenAmount"]?.toBigDecimalOrNull()
            ?: return WebhookResult.Error("Missing or invalid tokenAmount in metadata")

        val idempotencyKey = paymentIntent.metadata["idempotencyKey"]
            ?: return WebhookResult.Error("Missing idempotencyKey in metadata")

        return try {
            val transaction = dbQuery {
                val wallet = walletService.creditBalance(
                    userId = userId,
                    amount = tokenAmount,
                    type = TransactionType.PURCHASE,
                    idempotencyKey = idempotencyKey
                )

                transactionService.updateTransactionStatus(
                    idempotencyKey,
                    TransactionStatus.COMPLETED, null, null, wallet.tokenBalance
                ) ?: throw NotFoundException("Can't find transaction.")
            }

            logger.info ( "Payment succeeded for user $userId: $tokenAmount tokens credited" )
            WebhookResult.Success(transaction.transactionId)

        } catch (e: Exception) {
            logger.error("Failed to process payment success for user $userId" )
            WebhookResult.Error("Failed to credit wallet: ${e.message}")
        }
    }


    private suspend fun handlePaymentFailure(event: Event): WebhookResult {
        val dataObject = event.dataObjectDeserializer.rawJson

        // Use ApiResource.GSON to force-map the raw JSON to a PaymentIntent object
        val paymentIntent = ApiResource.GSON.fromJson(dataObject, PaymentIntent::class.java)
            ?: return WebhookResult.Error("Failed to parse raw JSON to PaymentIntent")

        val idempotencyKey = paymentIntent.metadata["idempotencyKey"]
            ?: return WebhookResult.Error("Missing idempotencyKey in metadata")
        val userId = paymentIntent.metadata["userId"] ?: return WebhookResult.Error("Missing userId")
        val errorMessage = paymentIntent.lastPaymentError?.message ?: "Payment failed"

        logger.warn ( "Payment failed for user $userId: $errorMessage" )

        // Update pending transaction to FAILED status
        val transaction = dbQuery{
            transactionService.updateTransactionStatus(
                idempotencyKey,
                TransactionStatus.FAILED, null, errorMessage, null
            ) ?: throw NotFoundException("Can't find transaction.")
        }
        // TODO: Send notification to user

        return WebhookResult.Failure(errorMessage)
    }


    private suspend fun handlePaymentCancelled(event: Event): WebhookResult {
        val dataObject = event.dataObjectDeserializer.rawJson

        // Use ApiResource.GSON to force-map the raw JSON to a PaymentIntent object
        val paymentIntent = ApiResource.GSON.fromJson(dataObject, PaymentIntent::class.java)
            ?: return WebhookResult.Error("Failed to parse raw JSON to PaymentIntent")

        val userId = paymentIntent.metadata["userId"] ?: return WebhookResult.Error("Missing userId")
        val idempotencyKey = paymentIntent.metadata["idempotencyKey"]
            ?: return WebhookResult.Error("Missing idempotencyKey in metadata")

        logger.info ( "Payment cancelled for user $userId" )

        // Update pending transaction to CANCELLED status
        dbQuery{
            transactionService.updateTransactionStatus(
                idempotencyKey,
                TransactionStatus.CANCELLED, null, "Payment cancelled by user.", null
            ) ?: throw NotFoundException("Can't find transaction.")
        }

        return WebhookResult.Cancelled
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