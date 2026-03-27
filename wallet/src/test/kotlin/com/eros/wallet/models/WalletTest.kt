package com.eros.wallet.models

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletTest {

    // Test helper function
    private fun wallet(
        walletId: Long = 1L,
        userId: String = "user-123",
        tokenBalance: BigDecimal = 1000.toBigDecimal(),
        lifetimeSpent: BigDecimal = 500.toBigDecimal(),
        lifetimePurchased: BigDecimal = 1500.toBigDecimal(),
        currency: String = "GBP",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ) = Wallet(walletId,userId, tokenBalance, lifetimeSpent, lifetimePurchased, currency, createdAt, updatedAt)

    @Nested
    inner class HasSufficientBalance {
        @Test
        fun `hasSufficientBalance returns true when balance is greater than amount`() {
            val wallet = wallet(tokenBalance = 1000.toBigDecimal())
            assertTrue(wallet.hasSufficientBalance(500.toBigDecimal()))
        }

        @Test
        fun `hasSufficientBalance returns true when balance equals amount`() {
            val wallet = wallet(tokenBalance = 500.toBigDecimal())
            assertTrue(wallet.hasSufficientBalance(500.toBigDecimal()))
        }

        @Test
        fun `hasSufficientBalance returns false when balance is less than amount`() {
            val wallet = wallet(tokenBalance = 300.toBigDecimal())
            assertFalse(wallet.hasSufficientBalance(500.toBigDecimal()))
        }

        @Test
        fun `hasSufficientBalance returns true for zero amount`() {
            val wallet = wallet(tokenBalance = 1000.toBigDecimal())
            assertTrue(wallet.hasSufficientBalance(0.toBigDecimal()))
        }

        @Test
        fun `hasSufficientBalance returns false when balance is zero`() {
            val wallet = wallet(tokenBalance = 0.toBigDecimal())
            assertFalse(wallet.hasSufficientBalance(100.toBigDecimal()))
        }
    }

    @Nested
    inner class PendingBalance {
        @Test
        fun `pendingBalance calculates sum of pending commitments`() {
            val wallet = wallet()
            val pending = listOf(100.toBigDecimal(), 200.toBigDecimal(), 150.toBigDecimal())

            assertEquals(450.toBigDecimal(), wallet.pendingBalance(pending))
        }

        @Test
        fun `pendingBalance returns zero for empty list`() {
            val wallet = wallet()
            assertEquals(0.toBigDecimal(), wallet.pendingBalance(emptyList()))
        }

        @Test
        fun `pendingBalance calculates sum with decimal values`() {
            val wallet = wallet()
            val pending = listOf(BigDecimal("1.50"), BigDecimal("2.75"), BigDecimal("3.25"))

            assertEquals(BigDecimal("7.50"), wallet.pendingBalance(pending))
        }

        @Test
        fun `pendingBalance handles single commitment`() {
            val wallet = wallet()
            val pending = listOf(250.toBigDecimal())

            assertEquals(250.toBigDecimal(), wallet.pendingBalance(pending))
        }
    }

    @Nested
    inner class Currency {
        @Test
        fun `default currency is GBP`() {
            val wallet = Wallet(
                walletId = 1L,
                userId = "user-123",
                tokenBalance = 1000.toBigDecimal(),
                lifetimeSpent = 500.toBigDecimal(),
                lifetimePurchased = 1500.toBigDecimal(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            assertEquals("GBP", wallet.currency)
        }

        @Test
        fun `can set custom currency`() {
            val wallet = wallet(currency = "USD")
            assertEquals("USD", wallet.currency)
        }
    }

    @Nested
    inner class LifetimeStats {
        @Test
        fun `lifetime purchased is greater than or equal to lifetime spent for valid wallet`() {
            val wallet = wallet(
                lifetimeSpent = 500.toBigDecimal(),
                lifetimePurchased = 1500.toBigDecimal()
            )

            assertTrue(wallet.lifetimePurchased >= wallet.lifetimeSpent)
        }

        @Test
        fun `token balance should not exceed lifetime purchased minus lifetime spent`() {
            val wallet = wallet(
                tokenBalance = 1000.toBigDecimal(),
                lifetimeSpent = 500.toBigDecimal(),
                lifetimePurchased = 1500.toBigDecimal()
            )

            val expectedMaxBalance = wallet.lifetimePurchased - wallet.lifetimeSpent
            assertTrue(wallet.tokenBalance <= expectedMaxBalance)
        }
    }

}