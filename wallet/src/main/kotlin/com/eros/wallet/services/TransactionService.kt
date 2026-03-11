package com.eros.wallet.services

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionHistory
import com.eros.wallet.models.TransactionHistoryResponse
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.toDTO
import com.eros.wallet.repository.TransactionRepository
import com.eros.wallet.repository.WalletRepository
import java.math.BigDecimal
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
    suspend fun findByIdempotencyKey(idempotencyKey: String): Transaction?{
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
    }

    /**
     * Service to find all the pending transactions for a user.
     */
    suspend fun findUserPendingTransactions(userId: String) : List<Transaction>{
        return transactionRepository.findPendingByUserId(userId)
    }


    /**
     * Service to find all the pending transactions for a user.
     */
    suspend fun findUserTransactionsForDate(userId: String, relatedDateId: Long) : List<Transaction>{
        return transactionRepository.findByUserIdAndDateId(userId, relatedDateId)
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
        tokenAmount: BigDecimal,
        newBalance: BigDecimal,
        amountPaidGBP: BigDecimal,
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
        amount: BigDecimal,
        description: String,
        relatedDateId: Long?,
        idempotencyKey: String,
        newBalance: BigDecimal,
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
        amount: BigDecimal,
        description: String,
        relatedDateId: Long?,
        relatedTransactionId: Long,
        newBalance: BigDecimal,
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
        offset: Int = 0,
        type: String? = null
    ): TransactionHistory {
       val history = if (type == null) {
            transactionRepository.findByUserId(userId)
        }else{
            transactionRepository.findByUserIdAndType(userId, TransactionType.valueOf(type))
        }
        val hasMore = history.size > limit+offset
        history.drop(offset).take(limit)
        return TransactionHistory(history, history.size, hasMore)
    }
}