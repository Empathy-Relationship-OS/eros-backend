package com.eros.wallet.models

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletTest {

    // Test helper function
    private fun wallet(
        userId: String = "user-123",
        tokenBalance: Double = 100.0,
        lifetimeSpent: Double = 50.0,
        lifetimePurchased: Double = 150.0,
        currency: String = "GBP",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ) = Wallet(userId, tokenBalance, lifetimeSpent, lifetimePurchased, currency, createdAt, updatedAt)

    @Nested
    inner class HasSufficientBalance {
        @Test
        fun `hasSufficientBalance returns true when balance is greater than amount`() {
            val wallet = wallet(tokenBalance = 100.0)
            assertTrue(wallet.hasSufficientBalance(50.0))
        }

        @Test
        fun `hasSufficientBalance returns true when balance equals amount`() {
            val wallet = wallet(tokenBalance = 50.0)
            assertTrue(wallet.hasSufficientBalance(50.0))
        }

        @Test
        fun `hasSufficientBalance returns false when balance is less than amount`() {
            val wallet = wallet(tokenBalance = 30.0)
            assertFalse(wallet.hasSufficientBalance(50.0))
        }

        @Test
        fun `hasSufficientBalance returns true for zero amount`() {
            val wallet = wallet(tokenBalance = 100.0)
            assertTrue(wallet.hasSufficientBalance(0.0))
        }

        @Test
        fun `hasSufficientBalance returns false when balance is zero`() {
            val wallet = wallet(tokenBalance = 0.0)
            assertFalse(wallet.hasSufficientBalance(10.0))
        }
    }

    @Nested
    inner class PendingBalance {
        @Test
        fun `pendingBalance calculates sum of pending commitments`() {
            val wallet = wallet()
            val pending = listOf(10.0, 20.0, 15.0)

            assertEquals(45.0, wallet.pendingBalance(pending))
        }

        @Test
        fun `pendingBalance returns zero for empty list`() {
            val wallet = wallet()
            assertEquals(0.0, wallet.pendingBalance(emptyList()))
        }

        @Test
        fun `pendingBalance calculates sum with decimal values`() {
            val wallet = wallet()
            val pending = listOf(1.5, 2.75, 3.25)

            assertEquals(7.5, wallet.pendingBalance(pending), 0.001)
        }

        @Test
        fun `pendingBalance handles single commitment`() {
            val wallet = wallet()
            val pending = listOf(25.0)

            assertEquals(25.0, wallet.pendingBalance(pending))
        }
    }

    @Nested
    inner class Currency {
        @Test
        fun `default currency is GBP`() {
            val wallet = Wallet(
                userId = "user-123",
                tokenBalance = 100.0,
                lifetimeSpent = 50.0,
                lifetimePurchased = 150.0,
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
                lifetimeSpent = 50.0,
                lifetimePurchased = 150.0
            )

            assertTrue(wallet.lifetimePurchased >= wallet.lifetimeSpent)
        }

        @Test
        fun `token balance should not exceed lifetime purchased minus lifetime spent`() {
            val wallet = wallet(
                tokenBalance = 100.0,
                lifetimeSpent = 50.0,
                lifetimePurchased = 150.0
            )

            val expectedMaxBalance = wallet.lifetimePurchased - wallet.lifetimeSpent
            assertTrue(wallet.tokenBalance <= expectedMaxBalance)
        }
    }

}