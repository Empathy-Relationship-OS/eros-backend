package com.eros.wallet.services

import com.eros.common.errors.BadRequestException
import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.createTestWallet
import com.eros.wallet.models.transaction
import com.eros.wallet.stripe.StripeService
import com.stripe.model.PaymentIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PaymentServiceTest {

    private val mockWalletService = mockk<WalletService>()
    private val mockTransactionService = mockk<TransactionService>()
    private val mockStripeService = mockk<StripeService>()
    private val paymentService = PaymentService(mockWalletService, mockTransactionService, mockStripeService)

    private val userId = "test-user-id"
    private val idempotencyKey = "test-key-123"

    @BeforeEach
    fun setup() {
        mockkStatic("com.eros.database.DatabasePluginKt")
        val mockTransaction = mockk<org.jetbrains.exposed.v1.core.Transaction>(relaxed = true)
        coEvery {
            dbQuery<Any>(any())
        } coAnswers {
            val block = firstArg<suspend org.jetbrains.exposed.v1.core.Transaction.() -> Any>()
            block.invoke(mockTransaction)
        }
    }

    // -------------------------------------------------------------------------
    // Idempotency
    // -------------------------------------------------------------------------

    @Test
    fun `purchaseTokens returns existing purchase when idempotency key already exists`() = runTest {
        val wallet = createTestWallet(userId = userId, currency = "usd")
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )
        val existingTransaction = transaction(
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.COMPLETED,
            stripePaymentIntentId = "pi_existing_123"
        )

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns existingTransaction

        val result = paymentService.purchaseTokens(userId, request)

        assertEquals("pi_existing_123", result.paymentIntentId)
        assertEquals(existingTransaction.amount, result.tokenAmount)
        // Stripe should never be called for a duplicate request
        coVerify(exactly = 0) { mockStripeService.createPaymentIntent(any(), any(), any(), any(), any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Wallet
    // -------------------------------------------------------------------------

    @Test
    fun `purchaseTokens throws NotFoundException when wallet does not exist`() = runTest {
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )

        coEvery { mockWalletService.getWallet(userId) } returns null

        assertThrows<NotFoundException> {
            paymentService.purchaseTokens(userId, request)
        }
    }

// -------------------------------------------------------------------------
// Package type
// -------------------------------------------------------------------------

    @Test
    fun `purchaseTokens throws BadRequestException for an invalid package type`() = runTest {
        val wallet = createTestWallet(userId = userId, currency = "usd")
        val request = PurchaseRequest(
            packageType = "INVALID_PACKAGE",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns null

        assertThrows<BadRequestException> {
            paymentService.purchaseTokens(userId, request)
        }
    }

    // -------------------------------------------------------------------------
    // Stripe failure
    // -------------------------------------------------------------------------

    @Test
    fun `purchaseTokens marks transaction as FAILED when Stripe throws`() = runTest {
        val wallet = createTestWallet(userId = userId, currency = "usd")
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )
        val pendingTransaction = transaction(
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.PENDING
        )

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns null
        coEvery {
            mockTransactionService.createPurchaseTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction
        coEvery {
            mockStripeService.createPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws RuntimeException("Stripe unavailable")
        coEvery {
            mockTransactionService.updateTransactionStatus(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction

        assertThrows<RuntimeException> {
            paymentService.purchaseTokens(userId, request)
        }

        // Verify the transaction was marked as failed with the error message
        coVerify {
            mockTransactionService.updateTransactionStatus(
                idempotencyKey,
                TransactionStatus.FAILED,
                null,
                "Stripe unavailable",
                null
            )
        }
    }

    @Test
    fun `purchaseTokens rethrows the original exception after marking transaction as failed`() = runTest {
        val wallet = createTestWallet(userId = userId, currency = "usd")
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )
        val pendingTransaction = transaction(
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.PENDING
        )
        val stripeException = RuntimeException("Card declined")

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns null
        coEvery {
            mockTransactionService.createPurchaseTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction
        coEvery {
            mockStripeService.createPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws stripeException
        coEvery {
            mockTransactionService.updateTransactionStatus(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction

        val thrown = assertThrows<RuntimeException> {
            paymentService.purchaseTokens(userId, request)
        }

        assertEquals("Card declined", thrown.message)
    }

    // -------------------------------------------------------------------------
    // Response correctness
    // -------------------------------------------------------------------------

    @Test
    fun `purchaseTokens passes correct currency from wallet to purchase`() = runTest {
        val wallet = createTestWallet(userId = userId, currency = "gbp")
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )
        val pendingTransaction = transaction(idempotencyKey = idempotencyKey, status = TransactionStatus.PENDING)
        val mockPaymentIntent = mockk<PaymentIntent> {
            every { id } returns "pi_test_123"
            every { clientSecret } returns "secret_123"
            every { amount } returns 799L
        }

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns null
        coEvery {
            mockTransactionService.createPurchaseTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction
        coEvery {
            mockStripeService.createPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockPaymentIntent
        coEvery {
            mockTransactionService.updateTransactionStatus(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction

        val result = paymentService.purchaseTokens(userId, request)

        assertEquals("gbp", result.currency)
    }

    @Test
    fun `purchaseTokens passes correct userId to createPaymentIntent`() = runTest {
        val wallet = createTestWallet(userId = userId, currency = "usd")
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )
        val pendingTransaction = transaction(idempotencyKey = idempotencyKey, status = TransactionStatus.PENDING)
        val mockPaymentIntent = mockk<PaymentIntent> {
            every { id } returns "pi_test_123"
            every { clientSecret } returns "secret_123"
            every { amount } returns 999L
        }

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns null
        coEvery {
            mockTransactionService.createPurchaseTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction
        coEvery {
            mockStripeService.createPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockPaymentIntent
        coEvery {
            mockTransactionService.updateTransactionStatus(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction

        paymentService.purchaseTokens(userId, request)

        coVerify {
            mockStripeService.createPaymentIntent(
                userId,
                any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `purchaseTokens returns a pending purchase for a valid request`() = runTest {

        val mockTransaction = mockk<org.jetbrains.exposed.v1.core.Transaction>(relaxed = true)

        mockkStatic("com.eros.database.DatabasePluginKt")
        coEvery {
            dbQuery<Any>(any())
        } coAnswers {
            val block = firstArg<suspend org.jetbrains.exposed.v1.core.Transaction.() -> Any>()
            block.invoke(mockTransaction)
        }

        val wallet = createTestWallet(userId = userId, currency = "usd")
        val request = PurchaseRequest(
            packageType = "STARTER",
            paymentMethodId = "pm_test_123",
            idempotencyKey = idempotencyKey,
            acceptedTerms = true
        )
        val pendingTransaction = transaction(
            idempotencyKey = idempotencyKey,
            status = TransactionStatus.PENDING
        )

        val mockPaymentIntent = mockk<PaymentIntent> {
            every { id } returns "pi_test_123"
            every { clientSecret } returns "secret_123"
            every { amount } returns 999L
        }

        coEvery { mockWalletService.getWallet(userId) } returns wallet
        coEvery { mockTransactionService.findByIdempotencyKey(idempotencyKey) } returns null
        coEvery {
            mockTransactionService.createPurchaseTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction
        coEvery {
            mockStripeService.createPaymentIntent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockPaymentIntent
        coEvery {
            mockTransactionService.updateTransactionStatus(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns pendingTransaction

        val result = paymentService.purchaseTokens(userId, request)

        assertEquals("pi_test_123", result.paymentIntentId)
        assertEquals("secret_123", result.clientSecret)
        assertEquals(TransactionStatus.PENDING.name, result.status)
        assertEquals(true, result.acceptedTerms)
    }
}