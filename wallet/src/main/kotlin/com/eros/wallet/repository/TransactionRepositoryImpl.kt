package com.eros.wallet.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.toTransactionDomain
import com.eros.wallet.table.Transactions
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class TransactionRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<Long, Transaction>(Transactions, Transactions.transactionId), TransactionRepository {

    override fun ResultRow.toDomain(): Transaction = toTransactionDomain()

    override fun toStatement(statement: UpdateBuilder<*>, entity: Transaction) {
        statement.apply {
            this[Transactions.userId] = entity.userId
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
        }
    }


    /**
     * Function for finding a transaction via idempotencyKey.
     */
    override suspend fun findByIdempotencyKey(idempotencyKey: String): Transaction? {
        return table.selectAll()
            .where { Transactions.idempotencyKey eq idempotencyKey }
            .singleOrNull()
            ?.toDomain()
    }


    /**
     * Function for finding all transactions for a specific user.
     */
    override suspend fun findByUserId(userId: String): List<Transaction> {
        return table.selectAll()
            .where { Transactions.userId eq userId }
            .map { it.toDomain() }
    }

    /**
     * Function for finding all transactions for a specific user with a given type.
     */
    override suspend fun findByUserIdAndType(userId: String, type: TransactionType): List<Transaction> {
        return table.selectAll()
            .where { (Transactions.userId eq userId) and (Transactions.type eq type) }
            .map { it.toDomain() }
    }

    /**
     * Function for finding all pending transactions for a specific user.
     */
    override suspend fun findPendingByUserId(userId: String): List<Transaction> {
        return table
            .selectAll().where {
                (Transactions.userId eq userId) and (Transactions.status eq TransactionStatus.PENDING) }
            .map { it.toTransactionDomain() }
    }


    /**
     * Function for finding by dateId for a given user.
     */
    override suspend fun findByUserIdAndDateId(userId: String, relatedDateId: Long): List<Transaction> {
        return table
            .selectAll().where {
                (Transactions.userId eq userId) and (Transactions.relatedDateId eq relatedDateId) }
            .map { it.toTransactionDomain() }
    }


    /**
     * Function to convert the metadata to a json.
     */
    private fun serializeMetadata(metadata: Map<String, String>): String? {
        return if (metadata.isEmpty()) null else Json.encodeToString(metadata)
    }
}