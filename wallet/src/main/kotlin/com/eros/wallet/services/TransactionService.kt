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
     * Runs [dbQuery] and should be done outside other dbQuery calls.
     *
     * @return [Transaction] if found, otherwise `null`.
     */
    suspend fun findByIdempotencyKey(idempotencyKey: String): Transaction? = dbQuery {
        transactionRepository.findByIdempotencyKey(idempotencyKey)
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
     * Function for finding if a given user has paid for a given date.
     */
    suspend fun hasUserAlreadyPaid(userId: String, dateId: Long): Boolean {
        return transactionRepository.hasUserAlreadyPaid(userId, dateId)
    }


    /**
     * Function to update the transaction status of a transaction.
     */
    suspend fun updateTransactionStatus(
        idempotencyKey: String,
        status: TransactionStatus,
        stripePaymentIntentId : String?,
        reason : String?,
        balanceAfter : BigDecimal?
    ) : Transaction? {
        return transactionRepository.updateTransactionStatus(idempotencyKey, status, stripePaymentIntentId, reason, balanceAfter)
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
     * Function for converting a pending transaction to COMPLETE.
     */
    suspend fun completePurchaseTransaction(
        stripePaymentIntentId: String,
        newBalance: BigDecimal
    ): Transaction? {
        // Find transaction.
        val transaction = transactionRepository.findByStripePaymentIntentId(stripePaymentIntentId)
            ?: throw NotFoundException("Transaction not found")

        // Update record and update in database.
        val updated = transaction.copy(
            status = TransactionStatus.COMPLETED,
            balanceAfter = newBalance
        )
        return transactionRepository.update(transaction.transactionId, updated)
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
        val paginated = history.drop(offset).take(limit)
        return TransactionHistory(paginated, paginated.size, hasMore)
    }
}