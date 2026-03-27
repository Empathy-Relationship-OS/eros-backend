package com.eros.wallet.models

import java.math.BigDecimal

//todo: Change from the default to real values
/**
 * Provides the cost of packages in GBP in pence.
 */
enum class TokenPackage(
    val tokens: BigDecimal,
    val priceGBP: BigDecimal,
) {
    STARTER(1.0.toBigDecimal(), 700.toBigDecimal()),      // £
    POPULAR(3.0.toBigDecimal(), 2000.toBigDecimal()),     // £
    PREMIUM(5.0.toBigDecimal(), 3250.toBigDecimal()),     // £
    MEGA(10.0.toBigDecimal(), 4500.toBigDecimal())        // £
}


/**
 * Provides the type of transaction.
 */
enum class TransactionType {
    PURCHASE,     // Token purchase via Stripe
    SPEND,        // Date commitment
    REFUND,       // Cancelled date refund
    ADJUSTMENT    // Manual admin adjustment
}


/**
 * Provides the status of a transaction.
 */
enum class TransactionStatus {
    PENDING,      // Payment intent created, awaiting confirmation
    COMPLETED,    // Successfully processed
    FAILED,       // Payment failed
    CANCELLED,    // Payment cancelled
    REFUNDED,     // Payment Refunded
    REFUND_FAILED // Refund failed
}


/**
 * Provides the type of refund.
 */
enum class RefundPolicy {
    FULL_REFUND,         // Partner cancelled: 100% back
    PARTIAL_REFUND,      // User cancelled >24h before: 50% back
    NO_REFUND            // User cancelled <24h before: 0% back
}