package com.eros.wallet.services

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.repository.TransactionRepository
import com.eros.wallet.repository.WalletRepository
import java.time.Clock
import java.time.Instant

class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * Service to find a record via the idempotency key.
     *
     * @return [Transaction] if found, otherwise `null`.
     */
    suspend fun findByIdempotencyKey(idempotencyKey: String): Transaction? = dbQuery {
        transactionRepository.findByIdempotencyKey(idempotencyKey)
    }


    /**
     * Service to create a purchase Transaction.
     *
     * @return [Transaction] once created.
     */
    /**
     * Creates a PURCHASE transaction.
     */
    suspend fun createPurchaseTransaction(
        userId: String,
        tokenAmount: Double,
        newBalance: Double,
        amountPaidGBP: Double,
        stripePaymentIntentId: String,
        idempotencyKey: String,
        status: TransactionStatus = TransactionStatus.PENDING,
        metadata: Map<String, String> = emptyMap()
    ): Transaction {
        val transaction = Transaction(
            transactionId = 0L,
            userId = userId,
            type = TransactionType.PURCHASE,
            amount = tokenAmount,
            balanceAfter = newBalance,
            description = "Purchased $tokenAmount tokens",
            status = status,
            relatedDateId = null,
            relatedTransactionId = null,
            stripePaymentIntentId = stripePaymentIntentId,
            amountPaidGBP = amountPaidGBP,
            idempotencyKey = idempotencyKey,
            metadata = metadata,
            createdAt = Instant.now(clock)
        )

        return transactionRepository.create(transaction)
    }


    /**
     * Service to spend credits. This is NOT wrapped in a dbQuery so is used within a dbQuery.
     *
     * @return Transaction of the spend transaction
     */
    suspend fun createSpendTransaction(
        userId: String,
        amount: Double,
        description: String,
        relatedDateId: Long?,
        idempotencyKey: String,
        newBalance: Double,
        metadata: Map<String, String>
    ): Transaction {
        val transaction = Transaction(
            transactionId = 0L,
            userId = userId,
            type = TransactionType.SPEND,
            amount = -amount,
            balanceAfter = newBalance,
            description = description,
            status = TransactionStatus.COMPLETED,
            relatedDateId = relatedDateId,
            relatedTransactionId = null,
            stripePaymentIntentId = null,
            amountPaidGBP = null,
            idempotencyKey = idempotencyKey,
            metadata = metadata,
            createdAt = Instant.now(clock)
        )
        return transactionRepository.create(transaction)
    }

    /**
     * Service to refund credits. This is NOT wrapped in a dbQuery so is used within a dbQuery.
     *
     * @return Transaction of the refund transaction
     */
    suspend fun createRefundTransaction(
        userId: String,
        amount: Double,
        description: String,
        relatedDateId: Long?,
        relatedTransactionId: Long,
        newBalance: Double,
        metadata: Map<String, String>
    ): Transaction {
        val transaction = Transaction(
            transactionId = 0L,
            userId = userId,
            type = TransactionType.REFUND,
            amount = amount,
            balanceAfter = newBalance,
            description = description,
            status = TransactionStatus.COMPLETED,
            relatedDateId = relatedDateId,
            relatedTransactionId = relatedTransactionId,
            stripePaymentIntentId = null,
            amountPaidGBP = null,
            idempotencyKey = null,
            metadata = metadata,
            createdAt = Instant.now(clock)
        )
        return transactionRepository.create(transaction)
    }

    /**
     * Gets transaction history for a user.
     */
    suspend fun getTransactionHistory(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): List<Transaction> {
        return transactionRepository.findByUserId(userId)
            .drop(offset)
            .take(limit)
    }
}