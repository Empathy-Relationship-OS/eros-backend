package com.eros.wallet.repository

import com.eros.common.errors.NotFoundException
import com.eros.database.repository.BaseDAOImpl
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.toTransactionDomain
import com.eros.wallet.table.Transactions
import com.eros.wallet.table.Wallets
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import kotlin.and
import kotlin.compareTo

class TransactionRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<Long, Transaction>(Transactions, Transactions.transactionId), TransactionRepository {

    override fun ResultRow.toDomain(): Transaction = toTransactionDomain()

    override fun toStatement(statement: UpdateBuilder<*>, entity: Transaction) {
        statement.apply {
            this[Transactions.walletId] = entity.walletId
            this[Transactions.type] = entity.type
            this[Transactions.amount] = entity.amount
            this[Transactions.balanceAfter] = entity.balanceAfter
            this[Transactions.description] = entity.description
            this[Transactions.status] = entity.status
            this[Transactions.relatedDateId] = entity.relatedDateId
            this[Transactions.relatedTransactionId] = entity.relatedTransactionId
            this[Transactions.stripePaymentIntentId] = entity.stripePaymentIntentId
            this[Transactions.amountPaidGbp] = entity.amountPaidGBP
            this[Transactions.idempotencyKey] = entity.idempotencyKey
            this[Transactions.metadata] = serializeMetadata(entity.metadata)
            this[Transactions.createdAt] = Instant.now(clock)
            this[Transactions.updatedAt] = Instant.now(clock)
        }
    }


    /**
     * Function for finding a transaction via idempotencyKey.
     */
    override suspend fun findByIdempotencyKey(idempotencyKey: String): Transaction? {
        return (table innerJoin Wallets).selectAll()
            .where { Transactions.idempotencyKey eq idempotencyKey }
            .singleOrNull()
            ?.toDomain()
    }


    /**
     * Function for finding all transactions for a specific user.
     */
    override suspend fun findByUserId(userId: String, limit: Int, offset: Long): List<Transaction> {
        return (table innerJoin Wallets).selectAll()
            .where { Wallets.userId eq userId }
            .limit(limit + 1)
            .offset(offset)
            .orderBy(Transactions.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    /**
     * Function for finding all transactions for a specific user with a given type.
     */
    override suspend fun findByUserIdAndType(
        userId: String,
        type: TransactionType?,
        limit: Int,
        offset: Long
    ): List<Transaction> {
        return (table innerJoin Wallets)
            .selectAll()
            .where {
                val conditions = mutableListOf<Op<Boolean>>()
                conditions.add(Wallets.userId eq userId)
                if (type != null) conditions.add(Transactions.type eq type)
                conditions.reduce { acc, op -> acc and op }
            }
            .limit(limit + 1)
            .offset(offset)
            .orderBy(Transactions.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    /**
     * Function for finding all pending transactions for a specific user.
     */
    override suspend fun findPendingByUserId(userId: String): List<Transaction> {
        return (table innerJoin Wallets)
            .selectAll().where {
                (Wallets.userId eq userId) and (Transactions.status eq TransactionStatus.PENDING)
            }
            .map { it.toTransactionDomain() }
    }


    /**
     * Function for finding by dateId for a given user.
     */
    override suspend fun findByUserIdAndDateId(userId: String, relatedDateId: Long): List<Transaction> {
        return (table innerJoin Wallets)
            .selectAll().where {
                (Wallets.userId eq userId) and (Transactions.relatedDateId eq relatedDateId)
            }
            .map { it.toTransactionDomain() }
    }


    /**
     * Function for finding a transaction based on stripe payment intent id.
     */
    override suspend fun findByStripePaymentIntentId(stripePaymentIntentId: String): Transaction? {
        return (table innerJoin Wallets).selectAll().where {
            Transactions.stripePaymentIntentId eq stripePaymentIntentId
        }
            .singleOrNull()?.toTransactionDomain()
    }

    /**
     * Function to determine if a user has paid for a date or not.
     */
    override suspend fun hasUserAlreadyPaid(userId: String, dateId: Long): Boolean {
        return (table innerJoin Wallets).selectAll().where {
            (Wallets.userId eq userId) and
                    (Transactions.relatedDateId eq dateId) and
                    (Transactions.type eq TransactionType.SPEND) and
                    (Transactions.status eq TransactionStatus.COMPLETED)
        }.empty().not()
    }

    /**
     * Function to update a transaction.
     */
    override suspend fun updateTransactionStatus(
        idempotencyKey: String,
        status: TransactionStatus,
        stripePaymentIntentId: String?,
        failureReason: String?,
        balanceAfter : BigDecimal?
    ): Transaction? {
        val updated = Transactions.updateReturning(
            where = { Transactions.idempotencyKey eq idempotencyKey }
        ) {
            it[Transactions.status] = status
            it[Transactions.createdAt] = Instant.now(clock)

            stripePaymentIntentId?.let { intentId ->
                it[Transactions.stripePaymentIntentId] = intentId
            }

            failureReason?.let { reason ->
                it[Transactions.metadata] = Json.encodeToString(
                    mapOf("failure_reason" to reason)
                )
            }

            balanceAfter?.let{ balance ->
                it[Transactions.balanceAfter] = balanceAfter
            }

            it[updatedAt] = Instant.now(clock)

        }
        return updated.singleOrNull()?.toDomain()
    }


    /**
     * Function to convert the metadata to a json.
     */
    private fun serializeMetadata(metadata: Map<String, String>): String? {
        return if (metadata.isEmpty()) null else Json.encodeToString(metadata)
    }
}