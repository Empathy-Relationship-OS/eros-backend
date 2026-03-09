package com.eros.wallet.models

import kotlinx.serialization.Serializable
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