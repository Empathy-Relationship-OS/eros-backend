package com.eros.wallet.models

import java.math.BigDecimal
import java.time.Instant

fun createTestWallet(
    walletId: Long = 1L,
    userId: String = "test-user-id",
    tokenBalance: BigDecimal = BigDecimal.ZERO,
    lifetimeSpent: BigDecimal = BigDecimal.ZERO,
    lifetimePurchased: BigDecimal = BigDecimal.ZERO,
    currency: String = "GBP",
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now()
): Wallet = Wallet(
    walletId = walletId,
    userId = userId,
    tokenBalance = tokenBalance,
    lifetimeSpent = lifetimeSpent,
    lifetimePurchased = lifetimePurchased,
    currency = currency,
    createdAt = createdAt,
    updatedAt = updatedAt
)
