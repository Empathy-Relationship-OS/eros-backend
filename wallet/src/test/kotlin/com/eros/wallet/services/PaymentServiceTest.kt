package com.eros.wallet.services

import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.Transaction
import com.eros.wallet.stripe.StripeService
import com.stripe.model.PaymentIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertNotNull

class PurchaseFlowTest {
    // Mocks
    private val stripeService = mockk<StripeService>()
    private val walletService = mockk<WalletService>()
    private val transactionService = mockk<TransactionService>()

    // The "Brain" we are testing
    private val orchestrator = PaymentService(
        walletService,
        transactionService,
        stripeService
    )

    //@Test
    fun `purchaseTokens should create a pending transaction and return it`() = runTest {
        // GIVEN
        val userId = "user_123"
        val request = PurchaseRequest(
            packageType = "GOLD_PACK",
            paymentMethodId = "pm_card_visa",
            idempotencyKey = "unique-key-123"
        )
        val mockIntent = mockk<PaymentIntent> {
            every { id } returns "pi_test_123"
        }

        // 1. Mock Idempotency Check (No existing transaction)
        coEvery { transactionService.findByIdempotencyKey(any()) } returns null

        // 2. Mock Stripe Intent Creation
        coEvery { stripeService.createPaymentIntent(any(), any(), any(), any(), any()) } returns mockIntent

        // 3. Mock Wallet Fetching
        coEvery { walletService.getWallet(userId) } returns mockk {
            every { tokenBalance } returns BigDecimal("50.00")
        }

        // 4. Mock the final DB save
        val expectedTx = mockk<Transaction>()
        coEvery { transactionService.createPurchaseTransaction(any(), any(), any(), any(), any(), any()) } returns expectedTx

        // WHEN
        val result = orchestrator.purchaseTokens(userId, request)

        // THEN
        assertNotNull(result)
        // Verify Stripe was called with correct params
        coVerify(exactly = 1) {
            stripeService.createPaymentIntent(userId, any(), "pm_card_visa", any(), any())
        }
        // Verify we saved a PENDING transaction with the Stripe ID
        coVerify(exactly = 1) {
            transactionService.createPurchaseTransaction(
                walletId = any(),
                tokenAmount = any(),
                newBalance = BigDecimal("50.00"),
                stripePaymentIntentId = "pi_test_123", // The ID from our mock
                idempotencyKey = "unique-key-123",
                amountPaidGBP = any()
            )
        }
    }
}