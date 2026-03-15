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
    STARTER(5.0.toBigDecimal(), 2500.toBigDecimal()),      // £25 - £5/$6 per token
    POPULAR(10.0.toBigDecimal(), 4500.toBigDecimal()),     // £45 - £4.50/$5.40 per token
    PREMIUM(20.0.toBigDecimal(), 8000.toBigDecimal()),     // £80 - £4/$4.80 per token
    MEGA(50.0.toBigDecimal(), 18000.toBigDecimal())       // £180 - £3.60/$4.20 per token
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
    CANCELLED     // Payment cancelled
}


/**
 * Provides the type of refund.
 */
enum class RefundPolicy {
    FULL_REFUND,         // Partner cancelled: 100% back
    PARTIAL_REFUND,      // User cancelled >24h before: 50% back
    NO_REFUND            // User cancelled <24h before: 0% back
}