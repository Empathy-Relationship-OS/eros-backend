package com.eros.wallet.services

import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.InsufficientBalanceException
import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.Wallet
import com.eros.wallet.models.transaction
import com.eros.wallet.models.wallet
import com.eros.wallet.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class WalletServiceTest {
    
    private val mockWalletRepo = mockk<WalletRepository>()
    private val mockTransactionService = mockk<TransactionService>()
    private val fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"))
    private val walletService = WalletService(mockWalletRepo, mockTransactionService, fixedClock)

    private val fixedNow = Instant.parse("2024-01-15T10:00:00Z")


    // ─────────────────────────────────────────────────────────────────────────
    // GetWallet
    // ─────────────────────────────────────────────────────────────────────────

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

    @Nested
    inner class GetWalletTests {

        @Test
        fun `should return wallet when found`() = runBlocking {
            setup()
            val wallet = wallet()
            coEvery { mockWalletRepo.findById("user-123") } returns wallet

            val result = walletService.getWallet("user-123")

            assertNotNull(result)
            assertEquals("user-123", result?.userId)
        }

        @Test
        fun `should return null when wallet not found`() = runBlocking {
            setup()
            coEvery { mockWalletRepo.findById("unknown") } returns null

            assertNull(walletService.getWallet("unknown"))
        }
    }
    
    
    @Nested
    inner class `Create Wallet` {

        @Test
        fun `successfully create wallet`() = runTest {
            setup()
            val userId = "user123"

            val expectedWallet = Wallet(
                walletId = 1L,
                userId = userId,
                tokenBalance = BigDecimal.ZERO.setScale(2),
                lifetimeSpent = BigDecimal.ZERO.setScale(2),
                lifetimePurchased = BigDecimal.ZERO.setScale(2),
                currency = "GBP",
                createdAt = fixedNow,
                updatedAt = fixedNow
            )

            coEvery { mockWalletRepo.doesExist(userId) } returns false
            coEvery { mockWalletRepo.create(any()) } returns expectedWallet

            val result = walletService.createWallet(userId)

            assertEquals(userId, result.userId)
            assertEquals(0, BigDecimal.ZERO.compareTo(result.tokenBalance))
            assertEquals("GBP", result.currency)
            assertEquals(fixedNow, result.createdAt)

            coVerify(exactly = 1) { mockWalletRepo.create(any()) }
        }

        @Test
        fun `throws exception when wallet already exists`() = runTest {
            setup()
            val userId = "user123"

            coEvery { mockWalletRepo.doesExist(userId) } returns true

            val exception = assertThrows<ForbiddenException> {
                walletService.createWallet(userId)
            }

            assertEquals("User $userId already has a wallet record.", exception.message)

            // Verify we NEVER tried to create the wallet
            coVerify(exactly = 0) { mockWalletRepo.create(any()) }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // CreditBalance
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class CreditBalanceTests {

        @Test
        fun `should credit balance and update lifetimePurchased for PURCHASE type`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"), lifetimePurchased = BigDecimal("100.0"))
            val updated = wallet.copy(tokenBalance = BigDecimal("200.0"), lifetimePurchased = BigDecimal("200.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockTransactionService.findByIdempotencyKey("ik-1") } returns null
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

            val result =walletService.creditBalance("user-1", BigDecimal("100.0"), TransactionType.PURCHASE, "ik-1")

            assertEquals(BigDecimal("200.0"), result.tokenBalance)
            assertEquals(BigDecimal("200.0"), result.lifetimePurchased)
        }

        @Test
        fun `should credit balance and update lifetimePurchased for REFUND type`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("50.0"), lifetimePurchased = BigDecimal("150.0"))
            val updated = wallet.copy(tokenBalance = BigDecimal("100.0"), lifetimePurchased = BigDecimal("200.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockTransactionService.findByIdempotencyKey("ik-2") } returns null
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

            val result =walletService.creditBalance("user-1", BigDecimal("50.0"), TransactionType.REFUND, "ik-2")

            assertEquals(BigDecimal("100.0"), result.tokenBalance)
        }

        @Test
        fun `should NOT update lifetimePurchased for SPEND type`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"), lifetimePurchased = BigDecimal("150.0"))
            val updated = wallet.copy(tokenBalance = BigDecimal("150.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockTransactionService.findByIdempotencyKey("ik-3") } returns null
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

           walletService.creditBalance("user-1", BigDecimal("50.0"), TransactionType.SPEND, "ik-3")

            coVerify {
                mockWalletRepo.update("user-1", match {
                    it.lifetimePurchased == BigDecimal("150.0") // unchanged
                })
            }
        }

        @Test
        fun `should throw ConflictException for zero amount`() = runBlocking {
            assertThrows<ConflictException> {
                runBlocking {
                   walletService.creditBalance("user-1", BigDecimal.ZERO, TransactionType.PURCHASE, "ik-4")
                }
            }
            Unit
        }

        @Test
        fun `should throw ConflictException for negative amount`() = runBlocking {
            assertThrows<ConflictException> {
                runBlocking {
                   walletService.creditBalance("user-1", BigDecimal("-10.0"), TransactionType.PURCHASE, "ik-5")
                }
            }
            Unit
        }

        @Test
        fun `should throw NotFoundException when wallet not found`() = runBlocking {
            coEvery { mockWalletRepo.findByIdForUpdate("ghost") } returns null

            assertThrows<NotFoundException> {
                runBlocking {
                   walletService.creditBalance("ghost", BigDecimal("50.0"), TransactionType.PURCHASE, "ik-6")
                }
            }
            Unit
        }

        @Test
        fun `should return existing wallet when idempotency key already completed`() = runBlocking {
            val wallet = wallet()
            val existingTx = transaction(status = TransactionStatus.COMPLETED, idempotencyKey = "ik-dup")

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockTransactionService.findByIdempotencyKey("ik-dup") } returns existingTx
            coEvery { mockWalletRepo.findById("user-1") } returns wallet

            val result =walletService.creditBalance("user-1", BigDecimal("50.0"), TransactionType.PURCHASE, "ik-dup")

            assertEquals(wallet.tokenBalance, result.tokenBalance)
            coVerify(exactly = 0) { mockWalletRepo.update(any(), any()) }
        }

        @Test
        fun `should proceed normally when idempotency key exists but is not COMPLETED`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"), lifetimePurchased = BigDecimal("100.0"))
            val pendingTx = transaction(status = TransactionStatus.PENDING, idempotencyKey = "ik-pending")
            val updated = wallet.copy(tokenBalance = BigDecimal("150.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockTransactionService.findByIdempotencyKey("ik-pending") } returns pendingTx
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

            val result =walletService.creditBalance("user-1", BigDecimal("50.0"), TransactionType.PURCHASE, "ik-pending")

            assertEquals(BigDecimal("150.0"), result.tokenBalance)
            coVerify(exactly = 1) { mockWalletRepo.update("user-1", any()) }
        }

        @Test
        fun `should use fixed clock timestamp on update`() = runBlocking {
            val wallet = wallet()
            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockTransactionService.findByIdempotencyKey("ik-7") } returns null
            coEvery { mockWalletRepo.update("user-1", any()) } returns wallet

           walletService.creditBalance("user-1", BigDecimal("10.0"), TransactionType.PURCHASE, "ik-7")

            coVerify { mockWalletRepo.update("user-1", match { it.updatedAt == fixedNow }) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DebitBalance
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class DebitBalanceTests {

        @Test
        fun `should debit balance correctly`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"))
            val updated = wallet.copy(tokenBalance = BigDecimal("70.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

            val result = walletService.debitBalance("user-1", BigDecimal("30.0"))

            assertEquals(BigDecimal("70.0"), result.tokenBalance)
            coVerify {
                mockWalletRepo.update("user-1", match {
                    it.tokenBalance == BigDecimal("70.0")
                })
            }
        }

        @Test
        fun `should throw ConflictException for zero amount`() = runBlocking {
            assertThrows<ConflictException> {
                runBlocking { walletService.debitBalance("user-1", BigDecimal.ZERO) }
            }
            Unit
        }

        @Test
        fun `should throw ConflictException for negative amount`() = runBlocking {
            assertThrows<ConflictException> {
                runBlocking { walletService.debitBalance("user-1", BigDecimal("-5.0")) }
            }
            Unit
        }

        @Test
        fun `should throw NotFoundException when wallet not found`() = runBlocking {
            coEvery { mockWalletRepo.findByIdForUpdate("ghost") } returns null

            assertThrows<NotFoundException> {
                runBlocking { walletService.debitBalance("ghost", BigDecimal("10.0")) }
            }
            Unit
        }

        @Test
        fun `should use fixed clock timestamp on update`() = runBlocking {
            val wallet = wallet()
            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockWalletRepo.update("user-1", any()) } returns wallet

            walletService.debitBalance("user-1", BigDecimal("10.0"))

            coVerify { mockWalletRepo.update("user-1", match { it.updatedAt == fixedNow }) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SpendTokens
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class SpendTokensTests {

        @Test
        fun `should deduct balance and increment lifetimeSpent`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"), lifetimeSpent = BigDecimal("50.0"))
            val updated = wallet.copy(tokenBalance = BigDecimal("60.0"), lifetimeSpent = BigDecimal("90.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

            val result = walletService.spendTokens("user-1", BigDecimal("40.0"))

            assertEquals(BigDecimal("60.0"), result.tokenBalance)
            assertEquals(BigDecimal("90.0"), result.lifetimeSpent)

            coVerify {
                mockWalletRepo.update("user-1", match {
                    it.tokenBalance == BigDecimal("60.0") &&
                            it.lifetimeSpent == BigDecimal("90.0")
                })
            }
        }

        @Test
        fun `should throw InsufficientBalanceException when balance is too low`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("10.0"))
            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet

            assertThrows<InsufficientBalanceException> {
                runBlocking { walletService.spendTokens("user-1", BigDecimal("50.0")) }
            }
            Unit
        }

        @Test
        fun `should throw ConflictException for zero amount`() = runBlocking {
            assertThrows<ConflictException> {
                runBlocking { walletService.spendTokens("user-1", BigDecimal.ZERO) }
            }
            Unit
        }

        @Test
        fun `should throw ConflictException for negative amount`() = runBlocking {
            assertThrows<ConflictException> {
                runBlocking { walletService.spendTokens("user-1", BigDecimal("-1.0")) }
            }
            Unit
        }

        @Test
        fun `should throw NotFoundException when wallet not found`() = runBlocking {
            coEvery { mockWalletRepo.findByIdForUpdate("ghost") } returns null

            assertThrows<NotFoundException> {
                runBlocking { walletService.spendTokens("ghost", BigDecimal("10.0")) }
            }
            Unit
        }

        @Test
        fun `should succeed when spending exact balance`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("50.0"), lifetimeSpent = BigDecimal("0.0"))
            val updated = wallet.copy(tokenBalance = BigDecimal("0.0"), lifetimeSpent = BigDecimal("50.0"))

            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockWalletRepo.update("user-1", any()) } returns updated

            val result = walletService.spendTokens("user-1", BigDecimal("50.0"))

            assertEquals(BigDecimal("0.0"), result.tokenBalance)
        }

        @Test
        fun `should use fixed clock timestamp on update`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"))
            coEvery { mockWalletRepo.findByIdForUpdate("user-1") } returns wallet
            coEvery { mockWalletRepo.update("user-1", any()) } returns wallet

            walletService.spendTokens("user-1", BigDecimal("10.0"))

            coVerify { mockWalletRepo.update("user-1", match { it.updatedAt == fixedNow }) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetBalance
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    inner class GetBalanceTests {

        @Test
        fun `should return balance with no pending transactions`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"))
            coEvery { mockWalletRepo.findById("user-1") } returns wallet
            coEvery { mockTransactionService.findUserPendingTransactions("user-1") } returns emptyList()

            val result = walletService.getBalance("user-1")

            assertEquals(BigDecimal("100.0"), result.tokenBalance)
            assertEquals(BigDecimal("100.0"), result.pendingTokenBalance)
        }

        @Test
        fun `should subtract pending spend amounts from pendingTokenBalance`() = runBlocking {
            val wallet = wallet(tokenBalance = BigDecimal("100.0"))
            val pending = listOf(
               transaction(type = TransactionType.SPEND, amount = BigDecimal("-30.0"), status = TransactionStatus.PENDING),
                transaction(type = TransactionType.SPEND, amount = BigDecimal("-20.0"), status = TransactionStatus.PENDING),
            )
            coEvery { mockWalletRepo.findById("user-1") } returns wallet
            coEvery { mockTransactionService.findUserPendingTransactions("user-1") } returns pending

            val result = walletService.getBalance("user-1")

            // pendingTokenBalance = 100 + (-30) + (-20) = 50
            assertEquals(BigDecimal("100.0"), result.tokenBalance)
            assertEquals(BigDecimal("50.0"), result.pendingTokenBalance)
        }

        @Test
        fun `should throw NotFoundException when wallet not found`() = runBlocking {
            coEvery { mockWalletRepo.findById("ghost") } returns null

            assertThrows<NotFoundException> {
                runBlocking { walletService.getBalance("ghost") }
            }
            Unit
        }

        @Test
        fun `should include lifetime fields in response`() = runBlocking {
            val wallet = wallet(
                tokenBalance = BigDecimal("100.0"),
                lifetimeSpent = BigDecimal("200.0"),
                lifetimePurchased = BigDecimal("300.0"),
                currency = "GBP"
            )
            coEvery { mockWalletRepo.findById("user-1") } returns wallet
            coEvery { mockTransactionService.findUserPendingTransactions("user-1") } returns emptyList()

            val result = walletService.getBalance("user-1")

            assertEquals(BigDecimal("200.0"), result.lifetimeSpent)
            assertEquals(BigDecimal("300.0"), result.lifetimePurchased)
            assertEquals("GBP", result.currency)
        }
    }

}