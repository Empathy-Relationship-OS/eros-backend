package com.eros.wallet.services

import com.eros.common.errors.ConflictException
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
    suspend fun findUserPendingTransactions(userId: String): List<Transaction> {
        return transactionRepository.findPendingByUserId(userId)
    }


    /**
     * Service to find all the pending transactions for a user.
     */
    suspend fun findUserTransactionsForDate(userId: String, relatedDateId: Long): List<Transaction> {
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
        stripePaymentIntentId: String?,
        reason: String?,
        balanceAfter: BigDecimal?
    ): Transaction? {
        return transactionRepository.updateTransactionStatus(
            idempotencyKey,
            status,
            stripePaymentIntentId,
            reason,
            balanceAfter
        )
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
        walletId: Long,
        tokenAmount: BigDecimal,
        newBalance: BigDecimal,
        amountPaidGBP: BigDecimal,
        stripePaymentIntentId: String?,
        idempotencyKey: String,
        status: TransactionStatus = TransactionStatus.PENDING,
        metadata: Map<String, String> = emptyMap(),
        acceptedTerms: Boolean
    ): Transaction {
        val transaction = Transaction(
            walletId = walletId,
            transactionId = 0L,
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
            acceptedTerms = acceptedTerms,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
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
        walletId: Long,
        userId: String,
        amount: BigDecimal,
        description: String,
        relatedDateId: Long?,
        idempotencyKey: String,
        newBalance: BigDecimal,
        metadata: Map<String, String>,
    ): Transaction {
        val transaction = Transaction(
            walletId = walletId,
            transactionId = 0L,
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
            acceptedTerms = null,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )
        return transactionRepository.create(transaction)
    }

    /**
     * Service to refund credits. This is NOT wrapped in a dbQuery so is used within a dbQuery.
     *
     * @return Transaction of the refund transaction
     */
    suspend fun createRefundTransaction(
        walletId: Long,
        userId: String,
        amount: BigDecimal,
        description: String,
        relatedDateId: Long?,
        relatedTransactionId: Long,
        newBalance: BigDecimal,
        metadata: Map<String, String>,
        acceptedTerms: Boolean?,
        refundIntent: String?,
        idempotencyKey: String?,
        status: TransactionStatus = TransactionStatus.PENDING
    ): Transaction {
        val transaction = Transaction(
            walletId = walletId,
            transactionId = 0L,
            type = TransactionType.REFUND,
            amount = amount,
            balanceAfter = newBalance,
            description = description,
            status = status,
            relatedDateId = relatedDateId,
            relatedTransactionId = relatedTransactionId,
            stripePaymentIntentId = refundIntent,
            amountPaidGBP = null,
            idempotencyKey = idempotencyKey,
            metadata = metadata,
            acceptedTerms = acceptedTerms,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )
        return transactionRepository.create(transaction)
    }

    /**
     * Gets transaction history for a user.
     */
    suspend fun getTransactionHistory(
        userId: String,
        limit: Int = 0,
        offset: Long = 0L,
        type: String? = null
    ): TransactionHistory {
        // 1. Fetch limit + 1 from the DB
        val dbResult = if (type == null) {
            transactionRepository.findByUserId(userId, limit, offset)
        } else {
            transactionRepository.findByUserIdAndType(userId, TransactionType.valueOf(type), limit, offset)
        }

        val hasMore = dbResult.size > limit
        val finalData = if (hasMore) dbResult.take(limit) else dbResult

        return TransactionHistory(
            transactions = finalData,
            total = finalData.size,
            hasMore = hasMore
        )
    }


    suspend fun getTransaction(transactionId: Long): Transaction? {
        return transactionRepository.getTransaction(transactionId)
    }


    suspend fun isRefundable(transaction: Transaction, userId: String): Boolean {
        // Check transaction isn't already refunded.
        if (transactionRepository.hasBeenRefunded(transaction.transactionId)) {
            return false
        }

        // Fetch users history.
        val allTransactions = transactionRepository.findByUserId(userId, 10000, 0)

        // Clean the history of all completed refund-loop pairs
        val refundRelatedIds = allTransactions
            .filter { it.type == TransactionType.REFUND && it.status == TransactionStatus.REFUNDED }
            .flatMap { listOfNotNull(it.transactionId, it.relatedTransactionId) }
            .toSet()
        val activeHistory = allTransactions
            .filterNot { it.transactionId in refundRelatedIds }
            .filterNot { it.type == TransactionType.REFUND && it.status == TransactionStatus.REFUNDED }
        if (activeHistory.isEmpty()) return false

        println("===========")
        println(activeHistory)

        if (transaction.type != TransactionType.PURCHASE) return false

        // Total spent in the wallet's history (using filtered activeHistory)
        val totalSpent = activeHistory
            .filter { it.type == TransactionType.SPEND }
            .sumOf { it.amount.abs() }

        // Sum of all purchases that happened BEFORE this specific one (using filtered activeHistory)
        val purchasesBeforeThisOne = activeHistory
            .filter { it.type == TransactionType.PURCHASE && it.createdAt < transaction.createdAt }
            .sumOf { it.amount }

        println("total spend: $totalSpent")
        println("purchasesBeforeThisOne:  $purchasesBeforeThisOne")

        return totalSpent <= purchasesBeforeThisOne
    }


    //suspend fun getTransactionsByStripeKey(stripeIntentId: String) : List<Transaction> {
    //    return transactionRepository.findByStripePaymentIntentId(stripeIntentId)
    //}

}