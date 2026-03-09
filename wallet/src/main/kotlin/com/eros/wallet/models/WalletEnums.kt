package com.eros.wallet.models

//todo: Change from the default to real values
/**
 * Provides the cost of packages in GBP in pence.
 */
enum class TokenPackage(
    val tokens: Double,
    val priceGBP: Int,
) {
    STARTER(5.0, 2500),      // £25 - £5/$6 per token
    POPULAR(10.0, 4500),     // £45 - £4.50/$5.40 per token
    PREMIUM(20.0, 8000),     // £80 - £4/$4.80 per token
    MEGA(50.0, 18000)       // £180 - £3.60/$4.20 per token
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
 * Provides the date activities along with their associated cost.
 */
enum class DateActivity(val tokenCost: Double) {
    DRINKS(1.0),          // Includes first drink
    COFFEE(0.5),          // Coffee shop
    WALK_TALK(0.5),       // Free activity, small commitment
    DINNER(1.5),          // Meal included
    MUSEUM(1.0),          // Entry ticket included
    ACTIVITY(1.5),        // Bowling, mini-golf, etc.
    BRUNCH(1.0)           // Weekend brunch
}


/**
 * Provides the type of refund.
 */
enum class RefundPolicy {
    FULL_REFUND,         // Partner cancelled: 100% back
    PARTIAL_REFUND,      // User cancelled >24h before: 50% back
    NO_REFUND            // User cancelled <24h before: 0% back
}