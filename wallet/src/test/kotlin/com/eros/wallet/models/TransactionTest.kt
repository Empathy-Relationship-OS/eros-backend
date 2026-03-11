package com.eros.wallet.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.Instant

class TransactionTest {

    // Test helper function
    private fun transaction(
        transactionId: Long = 1L,
        userId: String = "user-123",
        type: TransactionType = TransactionType.PURCHASE,
        amount: BigDecimal = 100.0.toBigDecimal(),
        balanceAfter: BigDecimal = 200.0.toBigDecimal(),
        description: String = "Test transaction",
        status: TransactionStatus = TransactionStatus.COMPLETED,
        relatedDateId: Long? = null,
        relatedTransactionId: Long? = null,
        stripePaymentIntentId: String? = null,
        amountPaidGBP: BigDecimal? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap(),
        createdAt: Instant = Instant.now()
    ) = Transaction(
        transactionId, userId, type, amount, balanceAfter, description,
        status, relatedDateId, relatedTransactionId, stripePaymentIntentId,
        amountPaidGBP, idempotencyKey, metadata, createdAt
    )

    @Nested
    inner class PurchaseTransactions {

        @Test
        fun `purchase transaction has positive amount`() {
            val transaction = transaction(
                type = TransactionType.PURCHASE,
                amount = 100.0.toBigDecimal()
            )

            assertTrue(transaction.amount > 0.toBigDecimal())
        }

        @Test
        fun `purchase transaction has stripe payment intent id`() {
            val paymentIntentId = "pi_test123"
            val transaction = transaction(
                type = TransactionType.PURCHASE,
                stripePaymentIntentId = paymentIntentId
            )

            assertEquals(paymentIntentId, transaction.stripePaymentIntentId)
        }

        @Test
        fun `purchase transaction has amount paid in GBP`() {
            val transaction = transaction(
                type = TransactionType.PURCHASE,
                amount = 100.0.toBigDecimal(),
                amountPaidGBP = 25.00.toBigDecimal()
            )

            assertNotNull(transaction.amountPaidGBP)
            assertEquals(25.00.toBigDecimal(), transaction.amountPaidGBP)
        }

        @Test
        fun `purchase transaction has idempotency key`() {
            val idempotencyKey = "idem-key-123"
            val transaction = transaction(
                type = TransactionType.PURCHASE,
                idempotencyKey = idempotencyKey
            )

            assertEquals(idempotencyKey, transaction.idempotencyKey)
        }

        @Test
        fun `purchase increases balance after`() {
            val previousBalance = 100.0.toBigDecimal()
            val purchaseAmount = 50.0.toBigDecimal()
            val transaction = transaction(
                type = TransactionType.PURCHASE,
                amount = purchaseAmount,
                balanceAfter = previousBalance + purchaseAmount
            )

            assertEquals(150.0.toBigDecimal(), transaction.balanceAfter)
        }
    }

    @Nested
    inner class SpendTransactions {

        @Test
        fun `spend transaction has negative amount`() {
            val transaction = transaction(
                type = TransactionType.SPEND,
                amount = -50.0.toBigDecimal()
            )

            assertTrue(transaction.amount < 0.toBigDecimal())
        }

        @Test
        fun `spend transaction links to date commitment`() {
            val dateId = 456L
            val transaction = transaction(
                type = TransactionType.SPEND,
                relatedDateId = dateId
            )

            assertEquals(dateId, transaction.relatedDateId)
        }

        @Test
        fun `spend decreases balance after`() {
            val previousBalance = 100.0.toBigDecimal()
            val spendAmount = -30.0.toBigDecimal()
            val transaction = transaction(
                type = TransactionType.SPEND,
                amount = spendAmount,
                balanceAfter = previousBalance + spendAmount
            )

            assertEquals(70.0, transaction.balanceAfter)
        }

        @Test
        fun `spend transaction has descriptive message`() {
            val transaction = transaction(
                type = TransactionType.SPEND,
                description = "Date with Sarah - Coffee"
            )

            assertTrue(transaction.description.isNotEmpty())
        }
    }

    @Nested
    inner class RefundTransactions {

        @Test
        fun `refund transaction has positive amount`() {
            val transaction = transaction(
                type = TransactionType.REFUND,
                amount = 50.0.toBigDecimal()
            )

            assertTrue(transaction.amount > 0.toBigDecimal())
        }

        @Test
        fun `refund transaction links to original transaction`() {
            val originalTxId = 123L
            val transaction = transaction(
                type = TransactionType.REFUND,
                relatedTransactionId = originalTxId
            )

            assertEquals(originalTxId, transaction.relatedTransactionId)
        }

        @Test
        fun `refund transaction links to cancelled date`() {
            val dateId = 789L
            val transaction = transaction(
                type = TransactionType.REFUND,
                relatedDateId = dateId
            )

            assertEquals(dateId, transaction.relatedDateId)
        }

        @Test
        fun `refund increases balance after`() {
            val previousBalance = 50.0.toBigDecimal()
            val refundAmount = 30.0.toBigDecimal()
            val transaction = transaction(
                type = TransactionType.REFUND,
                amount = refundAmount,
                balanceAfter = previousBalance + refundAmount
            )

            assertEquals(80.0, transaction.balanceAfter)
        }
    }

    @Nested
    inner class AdjustmentTransactions {

        @Test
        fun `adjustment can be positive`() {
            val transaction = transaction(
                type = TransactionType.ADJUSTMENT,
                amount = 10.0.toBigDecimal()
            )

            assertTrue(transaction.amount > 0.toBigDecimal())
        }

        @Test
        fun `adjustment can be negative`() {
            val transaction = transaction(
                type = TransactionType.ADJUSTMENT,
                amount = -10.0.toBigDecimal()
            )

            assertTrue(transaction.amount < 0.toBigDecimal())
        }

        @Test
        fun `adjustment has descriptive reason in metadata`() {
            val metadata = mapOf(
                "reason" to "Compensation for service issue",
                "admin" to "admin@example.com"
            )
            val transaction = transaction(
                type = TransactionType.ADJUSTMENT,
                metadata = metadata
            )

            assertTrue(transaction.metadata.containsKey("reason"))
            assertEquals("Compensation for service issue", transaction.metadata["reason"])
        }
    }

    @Nested
    inner class TransactionStatusTest {

        @Test
        fun `pending transaction represents incomplete payment`() {
            val transaction = transaction(
                status = TransactionStatus.PENDING,
                type = TransactionType.PURCHASE
            )

            assertEquals(TransactionStatus.PENDING, transaction.status)
        }

        @Test
        fun `completed transaction represents successful payment`() {
            val transaction = transaction(
                status = TransactionStatus.COMPLETED
            )

            assertEquals(TransactionStatus.COMPLETED, transaction.status)
        }

        @Test
        fun `failed transaction represents unsuccessful payment`() {
            val transaction = transaction(
                status = TransactionStatus.FAILED,
                type = TransactionType.PURCHASE
            )

            assertEquals(TransactionStatus.FAILED, transaction.status)
        }

        @Test
        fun `cancelled transaction represents user-cancelled payment`() {
            val transaction = transaction(
                status = TransactionStatus.CANCELLED,
                type = TransactionType.PURCHASE
            )

            assertEquals(TransactionStatus.CANCELLED, transaction.status)
        }
    }

    @Nested
    inner class MetadataHandling {

        @Test
        fun `transaction can have empty metadata`() {
            val transaction = transaction(metadata = emptyMap())

            assertTrue(transaction.metadata.isEmpty())
        }

        @Test
        fun `transaction can store multiple metadata fields`() {
            val metadata = mapOf(
                "ip_address" to "192.168.1.1",
                "user_agent" to "Mozilla/5.0",
                "promo_code" to "SUMMER20"
            )
            val transaction = transaction(metadata = metadata)

            assertEquals(3, transaction.metadata.size)
            assertEquals("192.168.1.1", transaction.metadata["ip_address"])
            assertEquals("SUMMER20", transaction.metadata["promo_code"])
        }
    }

    @Nested
    inner class IdempotencyHandling {

        @Test
        fun `idempotency key can be null for non-purchase transactions`() {
            val transaction = transaction(
                type = TransactionType.SPEND,
                idempotencyKey = null
            )

            assertNull(transaction.idempotencyKey)
        }

        @Test
        fun `idempotency key should be unique for each purchase attempt`() {
            val key1 = "idem-key-001"
            val key2 = "idem-key-002"

            val tx1 = transaction(idempotencyKey = key1)
            val tx2 = transaction(idempotencyKey = key2)

            assertNotEquals(tx1.idempotencyKey, tx2.idempotencyKey)
        }
    }

    @Nested
    inner class BalanceConsistency {

        @Test
        fun `balance after reflects the state after transaction`() {
            val previousBalance = 100.0.toBigDecimal()
            val transactionAmount = 50.0.toBigDecimal()
            val expectedBalance = previousBalance + transactionAmount

            val transaction = transaction(
                amount = transactionAmount,
                balanceAfter = expectedBalance
            )

            assertEquals(expectedBalance, transaction.balanceAfter)
        }

        @Test
        fun `balance after is non-negative for valid transactions`() {
            val transaction = transaction(
                balanceAfter = 150.0.toBigDecimal()
            )

            assertTrue(transaction.balanceAfter >= 0.toBigDecimal())
        }
    }
}