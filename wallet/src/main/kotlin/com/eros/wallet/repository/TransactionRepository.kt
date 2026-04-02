package com.eros.wallet.repository

import com.eros.database.repository.IBaseDAO
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import java.math.BigDecimal
import java.time.Instant

interface TransactionRepository : IBaseDAO<Long, Transaction> {

    suspend fun findByIdempotencyKey(idempotencyKey: String): Transaction?
    suspend fun findByUserId(userId: String, limit: Int, offset: Long): List<Transaction>
    suspend fun findPendingByUserId(userId: String): List<Transaction>
    suspend fun findByUserIdAndDateId(userId: String, relatedDateId: Long): List<Transaction>

    suspend fun findByUserIdAndType(userId: String, type: TransactionType?, limit: Int, offset: Long): List<Transaction>

    suspend fun findByStripePaymentIntentId(stripePaymentIntentId: String): Transaction?
    suspend fun hasUserAlreadyPaid(userId: String, dateId: Long): Boolean

    suspend fun findUserTransactionsAfterTime(createdAt: Instant, userId: String, type: TransactionType) : List<Transaction>

    suspend fun hasBeenRefunded(transactionId: Long) : Boolean

    suspend fun getTransaction(transactionId: Long) : Transaction?

    suspend fun updateTransactionStatus(
        idempotencyKey: String,
        status: TransactionStatus,
        stripePaymentIntentId: String?,
        failureReason: String?,
        balanceAfter: BigDecimal?
    ): Transaction?
}