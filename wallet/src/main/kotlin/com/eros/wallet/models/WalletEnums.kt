package com.eros.wallet.models

enum class TransactionType {
    PURCHASE,     // Token purchase via Stripe
    SPEND,        // Date commitment
    REFUND,       // Cancelled date refund
    ADJUSTMENT    // Manual admin adjustment
}

enum class TransactionStatus {
    PENDING,      // Payment intent created, awaiting confirmation
    COMPLETED,    // Successfully processed
    FAILED,       // Payment failed
    CANCELLED     // Payment cancelled
}