package com.eros.wallet.models

import com.eros.wallet.table.Transactions
import com.fasterxml.jackson.annotation.JsonFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import java.math.BigDecimal
import java.time.Instant

/**
 * Domain Object for a transaction.
 */
data class Transaction(
    val transactionId: Long,
    val walletId: Long,
    val type: TransactionType,
    val amount: BigDecimal, // Positive for credit, negative for debit
    val balanceAfter: BigDecimal,
    val description: String,
    val status: TransactionStatus,
    val relatedDateId: Long? = null,
    val relatedTransactionId: Long? = null, // For refunds
    val stripePaymentIntentId: String? = null,
    val amountPaidGBP: BigDecimal? = null,
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val acceptedTerms: Boolean?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Transaction history domain object.
 */
data class TransactionHistory(
    val transactions: List<Transaction>,
    val total: Int,
    val hasMore: Boolean
)


/**
 * Transaction history DTO.
 */
@Serializable
data class TransactionHistoryResponse(
    val transactions: List<TransactionResponse>,
    val total: Int,
    val hasMore: Boolean
)

fun TransactionHistory.toDTO() = TransactionHistoryResponse(
    transactions = this.transactions.map{it.toDTO()},
    total = this.total,
    hasMore = hasMore
)

/**
 * Transaction DTO.
 */
@Serializable
data class TransactionResponse(
    val transactionId: Long,
    val type: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val balanceAfter: BigDecimal,
    val description: String,
    val relatedDateId: Long? = null,
    val stripePaymentIntentId: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amountPaid: BigDecimal? = null,
    val acceptedTerms: Boolean?,
    val createdAt: String,
    val updatedAt: String,
    val status: String
)


fun Transaction.toDTO() = TransactionResponse(
    transactionId = this.transactionId,
    type = this.type.toString(),
    amount = this.amount,
    balanceAfter = this.balanceAfter,
    description = this.description,
    relatedDateId = this.relatedDateId,
    stripePaymentIntentId = this.stripePaymentIntentId,
    amountPaid = this.amountPaidGBP,
    acceptedTerms = this.acceptedTerms,
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString(),
    status = this.status.toString()
)


fun ResultRow.toTransactionDomain(): Transaction {
    return Transaction(
        walletId = this[Transactions.walletId],
        transactionId = this[Transactions.transactionId],
        type = this[Transactions.type],
        amount = this[Transactions.amount],
        balanceAfter = this[Transactions.balanceAfter],
        description = this[Transactions.description],
        status = this[Transactions.status],
        relatedDateId = this[Transactions.relatedDateId],
        relatedTransactionId = this[Transactions.relatedTransactionId],
        stripePaymentIntentId = this[Transactions.stripePaymentIntentId],
        amountPaidGBP = this[Transactions.amountPaidGbp],
        idempotencyKey = this[Transactions.idempotencyKey],
        metadata = parseMetadata(this[Transactions.metadata]),
        acceptedTerms = this[Transactions.acceptedTerms],
        createdAt = this[Transactions.createdAt],
        updatedAt = this[Transactions.updatedAt],
    )
}


private fun parseMetadata(json: String?): Map<String, String> {
    return if (json.isNullOrBlank()) {
        emptyMap()
    } else {
        try {
            Json.decodeFromString<Map<String, String>>(json)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Invalid metadata JSON format", e)
        }
    }
}
