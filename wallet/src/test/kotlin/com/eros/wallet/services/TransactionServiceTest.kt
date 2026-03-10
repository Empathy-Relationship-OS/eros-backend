package com.eros.wallet.services

import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TransactionServiceTest {

    private val mockTransactionRepo = mockk<TransactionRepository>()
    private val fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"))
    private val service = TransactionService(mockTransactionRepo, fixedClock)

    @Nested
    inner class CreateSpendTransactionTests {

        @Test
        fun `should create spend transaction with correct values`() = runBlocking {
            val expectedTransaction = Transaction(
                transactionId = 1L,
                userId = "user-1",
                type = TransactionType.SPEND,
                amount = -50.0,
                balanceAfter = 50.0,
                description = "Date commitment",
                status = TransactionStatus.COMPLETED,
                relatedDateId = 123L,
                idempotencyKey = "key-123",
                createdAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockTransactionRepo.create(any()) } returns expectedTransaction

            val result = service.createSpendTransaction(
                userId = "user-1",
                amount = 50.0,
                newBalance = 50.0,
                description = "Date commitment",
                relatedDateId = 123L,
                idempotencyKey = "key-123",
                metadata = emptyMap()
            )

            assertEquals(-50.0, result.amount)  // Negative for spend
            assertEquals(TransactionType.SPEND, result.type)
            assertEquals(TransactionStatus.COMPLETED, result.status)
            assertNull(result.stripePaymentIntentId)
            assertNull(result.amountPaidGBP)

            coVerify {
                mockTransactionRepo.create(match {
                    it.amount == -50.0 &&
                            it.type == TransactionType.SPEND &&
                            it.relatedDateId == 123L
                })
            }
        }
    }

    @Nested
    inner class CreatePurchaseTransactionTests {

        @Test
        fun `should create purchase transaction with stripe details`()  = runBlocking {
            val expectedTransaction = Transaction(
                transactionId = 1L,
                userId = "user-1",
                type = TransactionType.PURCHASE,
                amount = 100.0,
                balanceAfter = 100.0,
                description = "Purchased 100.0 tokens",
                status = TransactionStatus.PENDING,
                stripePaymentIntentId = "pi_123",
                amountPaidGBP = 45.00,
                idempotencyKey = "key-123",
                createdAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockTransactionRepo.create(any()) } returns expectedTransaction

            val result = service.createPurchaseTransaction(
                userId = "user-1",
                tokenAmount = 100.0,
                newBalance = 100.0,
                amountPaidGBP = 45.00,
                stripePaymentIntentId = "pi_123",
                idempotencyKey = "key-123",
                metadata = emptyMap()
            )

            assertEquals(100.0, result.amount)  // Positive for purchase
            assertEquals(TransactionType.PURCHASE, result.type)
            assertEquals("pi_123", result.stripePaymentIntentId)
            assertEquals(45.00, result.amountPaidGBP)
            assertNull(result.relatedDateId)
        }
    }

    @Nested
    inner class CreateRefundTransactionTests {

        @Test
        fun `should create refund transaction linking to original`()  = runBlocking  {
            val expectedTransaction = Transaction(
                transactionId = 2L,
                userId = "user-1",
                type = TransactionType.REFUND,
                amount = 50.0,
                balanceAfter = 100.0,
                description = "Refund for cancelled date",
                status = TransactionStatus.COMPLETED,
                relatedDateId = 123L,
                relatedTransactionId = 1L,
                createdAt = Instant.parse("2024-01-15T10:00:00Z")
            )

            coEvery { mockTransactionRepo.create(any()) } returns expectedTransaction

            val result = service.createRefundTransaction(
                userId = "user-1",
                amount = 50.0,
                newBalance = 100.0,
                description = "Refund for cancelled date",
                relatedDateId = 123L,
                relatedTransactionId = 1L,
                metadata = emptyMap()
            )

            assertEquals(50.0, result.amount)  // Positive for refund
            assertEquals(TransactionType.REFUND, result.type)
            assertEquals(123L, result.relatedDateId)
            assertEquals(1L, result.relatedTransactionId)
            assertNull(result.stripePaymentIntentId)
        }
    }

}