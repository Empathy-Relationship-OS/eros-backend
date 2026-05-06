package com.eros.wallet.services

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TransactionServiceTest {

    private val mockTransactionRepo = mockk<TransactionRepository>()
    private val fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"))
    private val service = TransactionService(mockTransactionRepo, fixedClock)
    val mockTransaction = mockk<org.jetbrains.exposed.v1.core.Transaction>(relaxed = true)

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun makeTransaction(
        transactionId: Long = 1L,
        walletId: Long = 1L,
        type: TransactionType = TransactionType.SPEND,
        amount: BigDecimal = BigDecimal("50.0"),
        balanceAfter: BigDecimal = BigDecimal("50.0"),
        description: String = "Test transaction",
        status: TransactionStatus = TransactionStatus.COMPLETED,
        relatedDateId: Long? = null,
        relatedTransactionId: Long? = null,
        stripePaymentIntentId: String? = null,
        amountPaidGBP: BigDecimal? = null,
        idempotencyKey: String? = "key-123",
        acceptedTerms: Boolean? = null,
        createdAt: Instant = Instant.parse("2024-01-15T10:00:00Z"),
        updatedAt: Instant = Instant.parse("2024-01-15T10:00:00Z"),
    ) = Transaction(
        transactionId = transactionId,
        walletId = walletId,
        type = type,
        amount = amount,
        balanceAfter = balanceAfter,
        description = description,
        status = status,
        relatedDateId = relatedDateId,
        relatedTransactionId = relatedTransactionId,
        stripePaymentIntentId = stripePaymentIntentId,
        amountPaidGBP = amountPaidGBP,
        idempotencyKey = idempotencyKey,
        acceptedTerms = acceptedTerms,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // CreateSpendTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class CreateSpendTransactionTests {

        @Test
        fun `should persist timestamps from fixed clock`() = runBlocking {
            val expected = makeTransaction(type = TransactionType.SPEND, amount = BigDecimal("-10.0"))
            coEvery { mockTransactionRepo.create(any()) } returns expected

            service.createSpendTransaction(
                walletId = 1L,
                userId = "user-1",
                amount = BigDecimal("10.0"),
                newBalance = BigDecimal("90.0"),
                description = "Lunch",
                relatedDateId = null,
                idempotencyKey = "ik-1",
                metadata = emptyMap()
            )

            coVerify {
                mockTransactionRepo.create(match {
                    it.createdAt == Instant.parse("2024-01-15T10:00:00Z") &&
                            it.updatedAt == Instant.parse("2024-01-15T10:00:00Z")
                })
            }
        }

        @Test
        fun `should allow null relatedDateId for spend transaction`() = runBlocking {
            val expected = makeTransaction(type = TransactionType.SPEND, amount = BigDecimal("-20.0"), relatedDateId = null)
            coEvery { mockTransactionRepo.create(any()) } returns expected

            val result = service.createSpendTransaction(
                walletId = 1L,
                userId = "user-1",
                amount = BigDecimal("20.0"),
                newBalance = BigDecimal("80.0"),
                description = "No date",
                relatedDateId = null,
                idempotencyKey = "ik-2",
                metadata = emptyMap()
            )

            assertNull(result.relatedDateId)
        }

        @Test
        fun `should create spend transaction with correct values`() = runBlocking {
            val expectedTransaction = Transaction(
                transactionId = 1L,
                walletId = 1L,
                type = TransactionType.SPEND,
                amount = (-50.0).toBigDecimal(),
                balanceAfter = 50.0.toBigDecimal(),
                description = "Date commitment",
                status = TransactionStatus.COMPLETED,
                relatedDateId = 123L,
                idempotencyKey = "key-123",
                acceptedTerms = true,
                createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                updatedAt = Instant.parse("2024-01-15T10:00:00Z"),
            )

            coEvery { mockTransactionRepo.create(any()) } returns expectedTransaction

            val result = service.createSpendTransaction(
                walletId = 1L,
                userId = "user-1",
                amount = 50.0.toBigDecimal(),
                newBalance = 50.0.toBigDecimal(),
                description = "Date commitment",
                relatedDateId = 123L,
                idempotencyKey = "key-123",
                metadata = emptyMap()
            )

            assertEquals((-50.0).toBigDecimal(), result.amount)  // Negative for spend
            assertEquals(TransactionType.SPEND, result.type)
            assertEquals(TransactionStatus.COMPLETED, result.status)
            assertNull(result.stripePaymentIntentId)
            assertNull(result.amountPaidGBP)

            coVerify {
                mockTransactionRepo.create(match {
                    it.amount == (-50.0).toBigDecimal() &&
                            it.type == TransactionType.SPEND &&
                            it.relatedDateId == 123L
                })
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CreatePurchaseTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class CreatePurchaseTransactionTests {

        @Test
        fun `should create purchase transaction with stripe details`() = runBlocking {
            val expected = makeTransaction(
                type = TransactionType.PURCHASE,
                amount = BigDecimal("100.0"),
                status = TransactionStatus.PENDING,
                stripePaymentIntentId = "pi_123",
                amountPaidGBP = BigDecimal("45.00"),
                idempotencyKey = "key-123",
                acceptedTerms = true,
            )
            coEvery { mockTransactionRepo.create(any()) } returns expected

            val result = service.createPurchaseTransaction(
                walletId = 1L,
                tokenAmount = BigDecimal("100.0"),
                newBalance = BigDecimal("100.0"),
                amountPaidGBP = BigDecimal("45.00"),
                stripePaymentIntentId = "pi_123",
                idempotencyKey = "key-123",
                acceptedTerms = true,
                metadata = emptyMap()
            )

            assertEquals(BigDecimal("100.0"), result.amount)
            assertEquals(TransactionType.PURCHASE, result.type)
            assertEquals(TransactionStatus.PENDING, result.status)
            assertEquals("pi_123", result.stripePaymentIntentId)
            assertEquals(BigDecimal("45.00"), result.amountPaidGBP)
            assertNull(result.relatedDateId)
        }

        @Test
        fun `should default status to PENDING for new purchase`() = runBlocking {
            val expected = makeTransaction(type = TransactionType.PURCHASE, status = TransactionStatus.PENDING)
            coEvery { mockTransactionRepo.create(any()) } returns expected

            service.createPurchaseTransaction(
                walletId = 1L,
                tokenAmount = BigDecimal("50.0"),
                newBalance = BigDecimal("50.0"),
                amountPaidGBP = BigDecimal("22.50"),
                stripePaymentIntentId = null,
                idempotencyKey = "ik-3",
                acceptedTerms = true,
                metadata = emptyMap()
            )

            coVerify {
                mockTransactionRepo.create(match { it.status == TransactionStatus.PENDING })
            }
        }

        @Test
        fun `should allow null stripePaymentIntentId`() = runBlocking {
            val expected = makeTransaction(type = TransactionType.PURCHASE, stripePaymentIntentId = null)
            coEvery { mockTransactionRepo.create(any()) } returns expected

            val result = service.createPurchaseTransaction(
                walletId = 1L,
                tokenAmount = BigDecimal("100.0"),
                newBalance = BigDecimal("100.0"),
                amountPaidGBP = BigDecimal("45.00"),
                stripePaymentIntentId = null,
                idempotencyKey = "ik-4",
                acceptedTerms = false,
                metadata = emptyMap()
            )

            assertNull(result.stripePaymentIntentId)
        }

        @Test
        fun `should pass description as Purchased N tokens`() = runBlocking {
            val expected = makeTransaction(
                type = TransactionType.PURCHASE,
                description = "Purchased 200.0 tokens"
            )
            coEvery { mockTransactionRepo.create(any()) } returns expected

            service.createPurchaseTransaction(
                walletId = 1L,
                tokenAmount = BigDecimal("200.0"),
                newBalance = BigDecimal("200.0"),
                amountPaidGBP = BigDecimal("90.00"),
                stripePaymentIntentId = "pi_456",
                idempotencyKey = "ik-5",
                acceptedTerms = true,
                metadata = emptyMap()
            )

            coVerify {
                mockTransactionRepo.create(match { it.description == "Purchased 200.0 tokens" })
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // CreateRefundTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class CreateRefundTransactionTests {

        @Test
        fun `should create refund transaction linking to original`() = runBlocking {
            val expected = makeTransaction(
                transactionId = 2L,
                type = TransactionType.REFUND,
                amount = BigDecimal("50.0"),
                balanceAfter = BigDecimal("100.0"),
                status = TransactionStatus.COMPLETED,
                relatedDateId = 123L,
                relatedTransactionId = 1L,
                acceptedTerms = true,
            )
            coEvery { mockTransactionRepo.create(any()) } returns expected

            val result = service.createRefundTransaction(
                walletId = 1L,
                userId = "user-1",
                amount = BigDecimal("50.0"),
                newBalance = BigDecimal("100.0"),
                description = "Refund for cancelled date",
                relatedDateId = 123L,
                relatedTransactionId = 1L,
                metadata = emptyMap(),
                acceptedTerms = true,
                refundIntent = "ri_20002dawadwefwaeswfsref",
                idempotencyKey = "ik__awawdgrDRGdrm54EFS3F"
            )

            assertEquals(BigDecimal("50.0"), result.amount)
            assertEquals(TransactionType.REFUND, result.type)
            assertEquals(123L, result.relatedDateId)
            assertEquals(1L, result.relatedTransactionId)
            assertNull(result.stripePaymentIntentId)
        }

        @Test
        fun `should default refund status to PENDING`() = runBlocking {
            val expected = makeTransaction(type = TransactionType.REFUND, status = TransactionStatus.PENDING)
            coEvery { mockTransactionRepo.create(any()) } returns expected

            service.createRefundTransaction(
                walletId = 1L,
                userId = "user-1",
                amount = BigDecimal("25.0"),
                newBalance = BigDecimal("75.0"),
                description = "Refund",
                relatedDateId = null,
                relatedTransactionId = 5L,
                metadata = emptyMap(),
                acceptedTerms = null,
                refundIntent = null,
                idempotencyKey = null
            )

            coVerify {
                mockTransactionRepo.create(match { it.status == TransactionStatus.PENDING })
            }
        }

        @Test
        fun `should allow null acceptedTerms and idempotencyKey`() = runBlocking {
            val expected = makeTransaction(
                type = TransactionType.REFUND,
                acceptedTerms = null,
                idempotencyKey = null
            )
            coEvery { mockTransactionRepo.create(any()) } returns expected

            val result = service.createRefundTransaction(
                walletId = 1L,
                userId = "user-1",
                amount = BigDecimal("30.0"),
                newBalance = BigDecimal("80.0"),
                description = "Partial refund",
                relatedDateId = null,
                relatedTransactionId = 7L,
                metadata = emptyMap(),
                acceptedTerms = null,
                refundIntent = null,
                idempotencyKey = null
            )

            assertNull(result.acceptedTerms)
            assertNull(result.idempotencyKey)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FindByIdempotencyKey
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class FindByIdempotencyKeyTests {

        @Test
        fun `should return transaction when found`() = runBlocking {
            mockkStatic("com.eros.database.DatabasePluginKt")
            coEvery {
                dbQuery<Any>(any())
            } coAnswers {
                val block = firstArg<suspend org.jetbrains.exposed.v1.core.Transaction.() -> Any>()
                block.invoke(mockTransaction)
            }

            val expected = makeTransaction(idempotencyKey = "ik-abc")
            coEvery { mockTransactionRepo.findByIdempotencyKey("ik-abc") } returns expected

            val result = service.findByIdempotencyKey("ik-abc")

            assertNotNull(result)
            assertEquals("ik-abc", result?.idempotencyKey)
        }

        @Test
        fun `should return null when not found`() = runBlocking {
            coEvery { mockTransactionRepo.findByIdempotencyKey("missing-key") } returns null

            val result = service.findByIdempotencyKey("missing-key")

            assertNull(result)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FindUserPendingTransactions
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class FindUserPendingTransactionsTests {

        @Test
        fun `should return pending transactions for user`() = runBlocking {
            val pending = listOf(
                makeTransaction(status = TransactionStatus.PENDING, idempotencyKey = "k1"),
                makeTransaction(status = TransactionStatus.PENDING, idempotencyKey = "k2"),
            )
            coEvery { mockTransactionRepo.findPendingByUserId("user-1") } returns pending

            val result = service.findUserPendingTransactions("user-1")

            assertEquals(2, result.size)
            assertTrue(result.all { it.status == TransactionStatus.PENDING })
        }

        @Test
        fun `should return empty list when no pending transactions`() = runBlocking {
            coEvery { mockTransactionRepo.findPendingByUserId("user-2") } returns emptyList()

            val result = service.findUserPendingTransactions("user-2")

            assertTrue(result.isEmpty())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FindUserTransactionsForDate
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class FindUserTransactionsForDateTests {

        @Test
        fun `should return transactions for a given date`() = runBlocking {
            val txns = listOf(
                makeTransaction(relatedDateId = 42L, idempotencyKey = "k1"),
                makeTransaction(relatedDateId = 42L, idempotencyKey = "k2"),
            )
            coEvery { mockTransactionRepo.findByUserIdAndDateId("user-1", 42L) } returns txns

            val result = service.findUserTransactionsForDate("user-1", 42L)

            assertEquals(2, result.size)
            assertTrue(result.all { it.relatedDateId == 42L })
        }

        @Test
        fun `should return empty list when no transactions exist for date`() = runBlocking {
            coEvery { mockTransactionRepo.findByUserIdAndDateId("user-1", 99L) } returns emptyList()

            val result = service.findUserTransactionsForDate("user-1", 99L)

            assertTrue(result.isEmpty())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HasUserAlreadyPaid
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class HasUserAlreadyPaidTests {

        @Test
        fun `should return true when user has already paid`() = runBlocking {
            coEvery { mockTransactionRepo.hasUserAlreadyPaid("user-1", 10L) } returns true

            assertTrue(service.hasUserAlreadyPaid("user-1", 10L))
        }

        @Test
        fun `should return false when user has not paid`() = runBlocking {
            coEvery { mockTransactionRepo.hasUserAlreadyPaid("user-1", 10L) } returns false

            assertFalse(service.hasUserAlreadyPaid("user-1", 10L))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UpdateTransactionStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class UpdateTransactionStatusTests {

        @Test
        fun `should update status and return updated transaction`() = runBlocking {
            val updated = makeTransaction(status = TransactionStatus.COMPLETED, stripePaymentIntentId = "pi_999")
            coEvery {
                mockTransactionRepo.updateTransactionStatus("ik-update", TransactionStatus.COMPLETED, "pi_999", null, BigDecimal("100.0"))
            } returns updated

            val result = service.updateTransactionStatus(
                idempotencyKey = "ik-update",
                status = TransactionStatus.COMPLETED,
                stripePaymentIntentId = "pi_999",
                reason = null,
                balanceAfter = BigDecimal("100.0")
            )

            assertNotNull(result)
            assertEquals(TransactionStatus.COMPLETED, result!!.status)
            assertEquals("pi_999", result.stripePaymentIntentId)
        }

        @Test
        fun `should return null when transaction not found`() = runBlocking {
            coEvery {
                mockTransactionRepo.updateTransactionStatus("missing", TransactionStatus.FAILED, null, "not found", null)
            } returns null

            val result = service.updateTransactionStatus(
                idempotencyKey = "missing",
                status = TransactionStatus.FAILED,
                stripePaymentIntentId = null,
                reason = "not found",
                balanceAfter = null
            )

            assertNull(result)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CompletePurchaseTransaction
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class CompletePurchaseTransactionTests {

        @Test
        fun `should complete a pending purchase transaction`() = runBlocking {
            val pending = makeTransaction(
                type = TransactionType.PURCHASE,
                status = TransactionStatus.PENDING,
                stripePaymentIntentId = "pi_abc",
                balanceAfter = BigDecimal("0.0")
            )
            val completed = pending.copy(status = TransactionStatus.COMPLETED, balanceAfter = BigDecimal("100.0"))

            coEvery { mockTransactionRepo.findByStripePaymentIntentId("pi_abc") } returns pending
            coEvery { mockTransactionRepo.update(1L, any()) } returns completed

            val result = service.completePurchaseTransaction("pi_abc", BigDecimal("100.0"))

            assertNotNull(result)
            assertEquals(TransactionStatus.COMPLETED, result!!.status)
            assertEquals(BigDecimal("100.0"), result.balanceAfter)
        }

        @Test
        fun `should throw NotFoundException when stripe intent not found`() = runBlocking {
            coEvery { mockTransactionRepo.findByStripePaymentIntentId("pi_unknown") } returns null

            assertThrows<NotFoundException> {
                runBlocking {
                    service.completePurchaseTransaction("pi_unknown", BigDecimal("100.0"))
                }
            }
            Unit
        }

        @Test
        fun `should call update with COMPLETED status and new balance`() = runBlocking {
            val pending = makeTransaction(
                type = TransactionType.PURCHASE,
                status = TransactionStatus.PENDING,
                stripePaymentIntentId = "pi_xyz"
            )
            val completed = pending.copy(status = TransactionStatus.COMPLETED, balanceAfter = BigDecimal("200.0"))

            coEvery { mockTransactionRepo.findByStripePaymentIntentId("pi_xyz") } returns pending
            coEvery { mockTransactionRepo.update(1L, any()) } returns completed

            service.completePurchaseTransaction("pi_xyz", BigDecimal("200.0"))

            coVerify {
                mockTransactionRepo.update(1L, match {
                    it.status == TransactionStatus.COMPLETED && it.balanceAfter == BigDecimal("200.0")
                })
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetTransactionHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class GetTransactionHistoryTests {

        @Test
        fun `should return transactions without hasMore when results fit within limit`() = runBlocking {
            val txns = (1..3).map { makeTransaction(transactionId = it.toLong(), idempotencyKey = "k$it") }
            coEvery { mockTransactionRepo.findByUserId("user-1", 20, 0L) } returns txns

            val result = service.getTransactionHistory("user-1")

            assertEquals(3, result.transactions.size)
            assertFalse(result.hasMore)
        }

        @Test
        fun `should indicate hasMore when db returns limit + 1 results`() = runBlocking {
            // Service requests limit+1 internally; simulate DB returning 21 rows for limit=20
            val txns = (1..21).map { makeTransaction(transactionId = it.toLong(), idempotencyKey = "k$it") }
            coEvery { mockTransactionRepo.findByUserId("user-1", 20, 0L) } returns txns

            val result = service.getTransactionHistory("user-1", limit = 20)

            assertEquals(20, result.transactions.size)
            assertTrue(result.hasMore)
        }

        @Test
        fun `should filter by type when type param provided`() = runBlocking {
            val purchases = listOf(
                makeTransaction(type = TransactionType.PURCHASE, idempotencyKey = "k1"),
            )
            coEvery {
                mockTransactionRepo.findByUserIdAndType("user-1", TransactionType.PURCHASE, 20, 0L)
            } returns purchases

            val result = service.getTransactionHistory("user-1", type = "PURCHASE")

            assertEquals(1, result.transactions.size)
            assertEquals(TransactionType.PURCHASE, result.transactions.first().type)
        }

        @Test
        fun `should respect custom limit and offset`() = runBlocking {
            coEvery { mockTransactionRepo.findByUserId("user-1", 5, 10L) } returns emptyList()

            val result = service.getTransactionHistory("user-1", limit = 5, offset = 10L)

            assertEquals(0, result.transactions.size)
            assertFalse(result.hasMore)
            coVerify { mockTransactionRepo.findByUserId("user-1", 5, 10L) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IsRefundable
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class IsRefundableTests {

        private val purchaseTime = Instant.parse("2024-01-10T10:00:00Z")
        private val spendTime = Instant.parse("2024-01-12T10:00:00Z")

        @Test
        fun `should return false when transaction has already been refunded`() = runBlocking {
            val tx = makeTransaction(transactionId = 1L, type = TransactionType.PURCHASE)
            coEvery { mockTransactionRepo.hasBeenRefunded(1L) } returns true

            assertFalse(service.isRefundable(tx, "user-1"))
        }

        @Test
        fun `should return false when transaction type is not PURCHASE`() = runBlocking {
            val tx = makeTransaction(transactionId = 2L, type = TransactionType.SPEND)
            coEvery { mockTransactionRepo.hasBeenRefunded(2L) } returns false
            coEvery { mockTransactionRepo.findByUserId("user-1", 10000, 0) } returns listOf(tx)

            assertFalse(service.isRefundable(tx, "user-1"))
        }

        @Test
        fun `should return false when history is empty after cleaning refund pairs`() = runBlocking {
            val refundTx = makeTransaction(
                transactionId = 10L,
                type = TransactionType.REFUND,
                status = TransactionStatus.REFUNDED,
                relatedTransactionId = 5L
            )
            val originalPurchase = makeTransaction(
                transactionId = 5L,
                type = TransactionType.PURCHASE,
                status = TransactionStatus.COMPLETED
            )
            coEvery { mockTransactionRepo.hasBeenRefunded(5L) } returns false
            coEvery { mockTransactionRepo.findByUserId("user-1", 10000, 0) } returns listOf(originalPurchase, refundTx)

            assertFalse(service.isRefundable(originalPurchase, "user-1"))
        }

        @Test
        fun `should return true when no tokens have been spent after purchase`() = runBlocking {
            val purchase = makeTransaction(
                transactionId = 1L,
                type = TransactionType.PURCHASE,
                amount = BigDecimal("100.0"),
                status = TransactionStatus.COMPLETED,
                createdAt = purchaseTime,
                idempotencyKey = "p1"
            )
            coEvery { mockTransactionRepo.hasBeenRefunded(1L) } returns false
            coEvery { mockTransactionRepo.findByUserId("user-1", 10000, 0) } returns listOf(purchase)

            assertTrue(service.isRefundable(purchase, "user-1"))
        }

        @Test
        fun `should return true when spend is less than or equal to prior purchases`() = runBlocking {
            val earlierPurchase = makeTransaction(
                transactionId = 1L,
                type = TransactionType.PURCHASE,
                amount = BigDecimal("200.0"),
                status = TransactionStatus.COMPLETED,
                createdAt = purchaseTime,
                idempotencyKey = "p1"
            )
            val targetPurchase = makeTransaction(
                transactionId = 2L,
                type = TransactionType.PURCHASE,
                amount = BigDecimal("100.0"),
                status = TransactionStatus.COMPLETED,
                createdAt = Instant.parse("2024-01-11T10:00:00Z"),
                idempotencyKey = "p2"
            )
            val spend = makeTransaction(
                transactionId = 3L,
                type = TransactionType.SPEND,
                amount = BigDecimal("-150.0"),
                status = TransactionStatus.COMPLETED,
                createdAt = spendTime,
                idempotencyKey = "s1"
            )
            coEvery { mockTransactionRepo.hasBeenRefunded(2L) } returns false
            coEvery { mockTransactionRepo.findByUserId("user-1", 10000, 0) } returns listOf(
                earlierPurchase, targetPurchase, spend
            )

            // totalSpent=150, purchasesBeforeTargetPurchase=200 → 150 <= 200 → refundable
            assertTrue(service.isRefundable(targetPurchase, "user-1"))
        }

        @Test
        fun `should return false when spend exceeds prior purchases`() = runBlocking {
            val purchase = makeTransaction(
                transactionId = 1L,
                type = TransactionType.PURCHASE,
                amount = BigDecimal("100.0"),
                status = TransactionStatus.COMPLETED,
                createdAt = purchaseTime,
                idempotencyKey = "p1"
            )
            val spend = makeTransaction(
                transactionId = 2L,
                type = TransactionType.SPEND,
                amount = BigDecimal("-150.0"),
                status = TransactionStatus.COMPLETED,
                createdAt = spendTime,
                idempotencyKey = "s1"
            )
            coEvery { mockTransactionRepo.hasBeenRefunded(1L) } returns false
            coEvery { mockTransactionRepo.findByUserId("user-1", 10000, 0) } returns listOf(purchase, spend)

            // totalSpent=150, purchasesBeforeThis=0 (no earlier purchases) → 150 > 0 → not refundable
            assertFalse(service.isRefundable(purchase, "user-1"))
        }
    }

}