package com.eros.wallet.services

import com.eros.common.errors.ForbiddenException
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.Wallet
import com.eros.wallet.repository.TransactionRepository
import com.eros.wallet.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class WalletServiceTest {

    private val mockTransactionRepo = mockk<TransactionRepository>()
    private val mockWalletRepo = mockk<WalletRepository>()
    private val fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"))
    private val transactionService = TransactionService(mockTransactionRepo, fixedClock)
    private val walletService = WalletService(mockWalletRepo, transactionService, fixedClock)


    @Nested
    inner class `Create Wallet` {

        @Test
        fun `successfully create wallet`() {
            runTest {

                val userId = "user123"

                val expectedWallet = Wallet(
                    userId = userId,
                    tokenBalance = BigDecimal.ZERO,
                    lifetimeSpent = BigDecimal.ZERO,
                    lifetimePurchased = BigDecimal.ZERO,
                    currency = "GBP",
                    createdAt = Instant.parse("2024-01-15T10:00:00Z"),
                    updatedAt = Instant.parse("2024-01-15T10:00:00Z")
                )

                coEvery { mockWalletRepo.doesExist(userId) } returns false
                coEvery { mockWalletRepo.create(any()) } returns expectedWallet

                val result = walletService.createWallet(userId)

                assertEquals(userId, result.userId)
                assertEquals(BigDecimal.ZERO, result.tokenBalance)
                assertEquals("GBP", result.currency)

                coVerify(exactly = 1) { mockWalletRepo.create(any()) }
            }
        }

        @Test
        fun `throws exception when wallet already exists`() = runTest {

            val userId = "user123"

            coEvery { mockWalletRepo.doesExist(userId) } returns true

            val exception = assertThrows<ForbiddenException> {
                runBlocking {
                    walletService.createWallet(userId)
                }
            }

            assertEquals("User $userId already has a wallet record.", exception.message)

            coVerify(exactly = 0) { mockWalletRepo.create(any()) }
        }
    }

}