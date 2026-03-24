package com.eros.wallet.services

import com.eros.common.DateActivity
import com.eros.common.errors.ConflictException
import com.eros.database.dbQuery
import com.eros.wallet.models.Purchase
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.SpendTokenRequest
import com.eros.wallet.models.TokenPackage
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionHistory
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.Wallet
import com.eros.wallet.models.WalletWithPending
import com.eros.wallet.stripe.StripeService
import io.ktor.server.plugins.NotFoundException

/**
 * This function is used from calling
 */
class PaymentService(
    private val walletService: WalletService,
    private val transactionService: TransactionService,
    private val stripeService: StripeService
) {

    /**
     * Function to get the balance of a user.
     */
    suspend fun getBalance(userId: String): WalletWithPending = dbQuery {
        walletService.getBalance(userId)
    }

    /**
     * Function to retrieve a users transaction history.
     */
    suspend fun getTransactionHistory(userId: String, limit: Int, offset: Long, type: String?): TransactionHistory =
        dbQuery {
            transactionService.getTransactionHistory(userId, limit, offset, type)
        }


    /**
     * This function is for refunding
     */
    suspend fun refundTokens(userId: String) {
        throw NotImplementedError("No refund in PaymentService.kt")
    }


    /**
     * This function is used for purchasing tokens.
     *
     * Note: DB Query is done separate so createPaymentIntent avoids blocking.
     */
    suspend fun purchaseTokens(userId: String, request: PurchaseRequest): Purchase {
        // Idempotency Check - Avoid duplicates.
        val existing = transactionService.findByIdempotencyKey(request.idempotencyKey)
        if (existing != null) return Purchase(
            clientSecret = existing.stripePaymentIntentId ?: "",
            paymentIntentId = existing.stripePaymentIntentId ?: "",
            amount = existing.amountPaidGBP ?: 0.toBigDecimal(),
            currency = "gbp",
            tokenAmount = existing.amount,
            status = existing.status.name,
            transactionId = existing.transactionId
        )

        val tokenPackage = TokenPackage.valueOf(request.packageType)

        // Create a pending transaction and return.
        val (transaction, wallet) = dbQuery {
            val wallet = walletService.getWallet(userId)
                ?: throw NotFoundException("Wallet not found")

            val transaction = transactionService.createPurchaseTransaction(
                walletId = wallet.walletId,
                tokenPackage.tokens,
                wallet.tokenBalance + tokenPackage.tokens,
                amountPaidGBP = tokenPackage.priceGBP,
                stripePaymentIntentId = null,
                idempotencyKey = request.idempotencyKey
            )
            transaction to wallet
        }

        // Try-catch to alter the transaction to failed if an error occurs
        try {
            // Create Payment Intent
            val paymentIntent = stripeService.createPaymentIntent(
                userId, tokenPackage,
                request.paymentMethodId, request.idempotencyKey,
                wallet.currency
            )

            // Update transaction and return purchase request.
            return dbQuery {
                transactionService.updateTransactionStatus(
                    request.idempotencyKey,
                    TransactionStatus.PENDING,
                    paymentIntent.id,
                    null, null
                )

                Purchase(
                    paymentIntent.clientSecret,
                    paymentIntent.id,
                    tokenPackage.priceGBP,
                    wallet.currency,
                    tokenPackage.tokens,
                    paymentIntent.status,
                    transactionId = transaction.transactionId
                )

            }
            //todo: I don't know what error paymentIntend might throw
        }catch(e: Exception){
            dbQuery {
                transactionService.updateTransactionStatus(
                    request.idempotencyKey,
                    TransactionStatus.FAILED,
                    null,
                    e.message, null
                )
            }
            throw e
        }
    }


    /**
     * This function is used for spending tokens internally for dates.
     */
    suspend fun spendToken(userId: String, request: SpendTokenRequest): Transaction {

        // Check the idempotency key for existing spend.
        val existing = transactionService.findByIdempotencyKey(request.idempotencyKey)
        if (existing != null) return existing

        return dbQuery {
            // Check the balance of the user is sufficient.
            val cost = DateActivity.valueOf(request.activity).tokenCost.toBigDecimal()
            val wallet = walletService.getWallet(userId) ?: throw NotFoundException("Can't find user $userId wallet.")
            if (wallet.tokenBalance < cost) {
                //todo: Change to correct exception
                throw Exception("User $userId has ${wallet.tokenBalance}, needs $cost")
            }

            // Ensure the user hasn't already paid for the date.
            val alreadyPaid = transactionService.findUserTransactionsForDate(
                userId = userId,
                relatedDateId = request.relatedDateId,
            ).filter { it.type == TransactionType.SPEND && it.status == TransactionStatus.COMPLETED }
            if (alreadyPaid.isNotEmpty()) throw IllegalStateException("Date already paid for")

            // Deduct the cost from the user.
            val updatedWallet = walletService.spendTokens(
                userId = userId,
                amount = cost,
            )

            // Create transaction of the user spend for the date.
            transactionService.createSpendTransaction(
                walletId = updatedWallet.walletId,
                userId = userId,
                amount = cost,
                description = "Paid for ${request.activity} date for $cost tokens",
                relatedDateId = request.relatedDateId,
                idempotencyKey = request.idempotencyKey,
                newBalance = updatedWallet.tokenBalance,
                metadata = mapOf(
                    "activity" to request.activity,
                    "relatedDateId" to request.relatedDateId.toString(),
                    "cost" to cost.toString()
                )
            )
        }
    }
}