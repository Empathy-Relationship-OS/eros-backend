package com.eros.wallet.models

import com.eros.wallet.table.Transactions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant


/**
 * Domain Object for a transaction.
 */
data class Transaction(
    val transactionId: Long,
    val userId: String,
    val type: TransactionType,
    val amount: Double, // Positive for credit, negative for debit
    val balanceAfter: Double,
    val description: String,
    val status: TransactionStatus,
    val relatedDateId: Long? = null,
    val relatedTransactionId: Long? = null, // For refunds
    val stripePaymentIntentId: String? = null,
    val amountPaidGBP: Double? = null,
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant
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

/**
 * Transaction DTO.
 */
@Serializable
data class TransactionResponse(
    val transactionId: String,
    val type: String,
    val amount: Double,
    val balanceAfter: Double,
    val description: String,
    val relatedDateId: String? = null,
    val stripePaymentIntentId: String? = null,
    val amountPaid: String? = null,
    val createdAt: String
)

fun ResultRow.toTransactionDomain(): Transaction {
    return Transaction(
        transactionId = this[Transactions.transactionId],
        userId = this[Transactions.userId],
        type = this[Transactions.type],
        amount = this[Transactions.amount].toDouble(),
        balanceAfter = this[Transactions.balanceAfter].toDouble(),
        description = this[Transactions.description],
        status = this[Transactions.status],
        relatedDateId = this[Transactions.relatedDateId],
        relatedTransactionId = this[Transactions.relatedTransactionId],
        stripePaymentIntentId = this[Transactions.stripePaymentIntentId],
        amountPaidGBP = this[Transactions.amountPaidGbp]?.toDouble(),
        idempotencyKey = this[Transactions.idempotencyKey],
        metadata = parseMetadata(this[Transactions.metadata]),
        createdAt = this[Transactions.createdAt]
    )
}

private fun parseMetadata(json: String?): Map<String, String> {
    return if (json.isNullOrBlank()) {
        emptyMap()
    } else {
        try {
            Json.decodeFromString<Map<String, String>>(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}