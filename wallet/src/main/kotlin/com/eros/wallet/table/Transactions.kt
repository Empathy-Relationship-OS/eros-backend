package com.eros.wallet.table

import com.eros.dates.tables.DateCommitments
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

// Database table
object Transactions : Table("transactions") {
    val transactionId = long("transaction_id").autoIncrement()
    val walletId = long("wallet_id").references(Wallets.walletId)
    val type = enumerationByName("type", 20, TransactionType::class)
    val amount = decimal("amount", 10, 2)
    val balanceAfter = decimal("balance_after", 10, 2)
    val description = text("description")
    val status = enumerationByName("status", 20, TransactionStatus::class)
    val relatedDateId = long("date_id").references(DateCommitments.dateId).nullable()
    val relatedTransactionId = long("related_transaction_id").references(transactionId).nullable()
    val stripePaymentIntentId = varchar("stripe_payment_intent_id", 255).nullable()
    val amountPaidGbp = decimal("amount_paid_gbp", 10, 2).nullable()
    val idempotencyKey = varchar("idempotency_key", 255).nullable().uniqueIndex()
    val metadata = text("metadata").nullable() // JSON
    val acceptedTerms = bool("accepted_terms").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(transactionId)

    init {
        index(false, walletId, createdAt)
        index(false, stripePaymentIntentId)
    }
}