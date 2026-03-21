package com.eros.wallet.services

import com.eros.common.errors.ConflictException
import com.eros.common.errors.ForbiddenException
import com.eros.common.errors.InsufficientBalanceException
import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.Purchase
import com.eros.wallet.models.PurchaseRequest
import com.eros.wallet.models.PurchaseResponse
import com.eros.wallet.models.RefundPolicy
import com.eros.wallet.models.TokenPackage
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionHistory
import com.eros.wallet.models.TransactionHistoryResponse
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.Wallet
import com.eros.wallet.models.WalletResponse
import com.eros.wallet.models.WalletWithPending
import com.eros.wallet.repository.WalletRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

class WalletService(
    private val walletRepository: WalletRepository,
    private val transactionService: TransactionService,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * Function to return the Wallet of the provided userId.
     *
     * @return [Wallet] of the userId provided, otherwise null
     */
    suspend fun getWallet(userId: String): Wallet? = dbQuery {
        walletRepository.findById(userId)
    }

    /**
     * Function for creating a wallet for a given userId and their currency.
     */
    suspend fun createWallet(userId: String, currency: String = "GBP"): Wallet = dbQuery {
        // Check if the wallet already exists
        if (walletRepository.doesExist(userId)) {
            throw ForbiddenException("User $userId already has a wallet record.")
        }

        // Create wallet
        val now = Instant.now(clock)
        val wallet = Wallet(
            userId = userId,
            tokenBalance = BigDecimal.ZERO,
            lifetimeSpent = BigDecimal.ZERO,
            lifetimePurchased = BigDecimal.ZERO,
            currency = currency,
            createdAt = now,
            updatedAt = now
        )

        // Create wallet in database
        walletRepository.create(wallet)
    }


    /**
     * Creates a purchase intent for token purchase.
     * Does NOT credit balance - that happens when Stripe webhook confirms payment.
     */
    suspend fun createPurchase(userId: String, request: PurchaseRequest): Purchase = dbQuery {
        // 1. Check idempotency
        transactionService.findByIdempotencyKey(request.idempotencyKey)?.let { existing ->
            // Already processed - return existing response
            return@dbQuery Purchase(
                clientSecret = existing.stripePaymentIntentId ?: "",
                paymentIntentId = existing.stripePaymentIntentId ?: "",
                amount = existing.amountPaidGBP ?: 0.toBigDecimal(),
                currency = "gbp",
                tokenAmount = existing.amount,
                status = existing.status.name,
                newBalance = if (existing.status == TransactionStatus.COMPLETED) {
                    walletRepository.findById(userId)?.tokenBalance
                } else null,
                transactionId = existing.transactionId
            )
        }

        // 2. Get wallet - with lock.
        val wallet = walletRepository.findByIdForUpdate(userId)
            ?: throw NotFoundException("Can't find user $userId's wallet.")

        // 3. Validate package type
        val pack = try {
            TokenPackage.valueOf(request.packageType)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid package type: ${request.packageType}")
        }

        // 4. Create pending transaction
        val transaction = transactionService.createPurchaseTransaction(
            userId = userId,
            tokenAmount = pack.tokens,
            newBalance = wallet.tokenBalance,
            amountPaidGBP = pack.priceGBP,
            stripePaymentIntentId = request.paymentMethodId,
            idempotencyKey = request.idempotencyKey,
            status = TransactionStatus.PENDING,
            metadata = emptyMap()
        )

        // 5. Return purchase
        Purchase(
            clientSecret = transaction.stripePaymentIntentId ?: "",
            paymentIntentId = transaction.stripePaymentIntentId ?: "",
            amount = pack.priceGBP,
            currency = "gbp",
            tokenAmount = pack.tokens,
            status = "pending",
            newBalance = null,  // Not credited yet
            transactionId = transaction.transactionId
        )
    }


    /**
     * Credits wallet balance (called by Stripe webhook or refund processing).
     */
    suspend fun creditBalance(
        userId: String,
        amount: BigDecimal,
        type: TransactionType,
        idempotencyKey: String,
    ): Wallet {
        // Check idempotency and return wallet if already found.
        val existingTx = transactionService.findByIdempotencyKey(idempotencyKey)
        if (existingTx != null && existingTx.status == TransactionStatus.COMPLETED) {
            return walletRepository.findById(userId)
                ?: throw NotFoundException("Wallet not found")
        }

        // Lock wallet
        val wallet = walletRepository.findByIdForUpdate(userId)
            ?: throw NotFoundException("Wallet not found")

        // Calculate new values
        val newBalance = wallet.tokenBalance + amount
        val newLifetimePurchased = when (type) {
            TransactionType.PURCHASE, TransactionType.REFUND ->
                wallet.lifetimePurchased + amount
            else -> wallet.lifetimePurchased
        }

        // Update and return new wallet
        return walletRepository.update(userId, wallet.copy(
            tokenBalance = newBalance,
            lifetimePurchased = newLifetimePurchased,
            updatedAt = Instant.now(clock)
        )) ?: throw Exception("Failed to update wallet")
    }


    /**
     * Spends tokens (for date commitments).
     */
    suspend fun spendTokens(
        userId: String,
        amount: BigDecimal,
    ): Wallet{

        // Lock wallet
        val wallet = walletRepository.findByIdForUpdate(userId)
            ?: throw NotFoundException("Can't find user $userId's wallet.")

        // Validate balance
        if (!wallet.hasSufficientBalance(amount)) {
            throw InsufficientBalanceException(
                currentBalance = wallet.tokenBalance,
                requiredAmount = amount
            )
        }

        // Calculate new values
        val newBalance = wallet.tokenBalance - amount
        val newLifetimeSpent = wallet.lifetimeSpent + amount

        //  Update wallet
        return walletRepository.update(userId, wallet.copy(
            tokenBalance = newBalance,
            lifetimeSpent = newLifetimeSpent,
            updatedAt = Instant.now(clock)
        )) ?: throw Exception("Failed to update wallet")
    }

    /**
     * Refunds tokens for cancelled dates.
     */
    suspend fun refundTokens(
        userId: String,
        relatedDateId: Long,
        refundPolicy: RefundPolicy
    ): WalletResponse {
        // 1. Lock wallet
        val wallet = walletRepository.findByIdForUpdate(userId)
            ?: throw NotFoundException("Wallet not found")

        // 2. Get completed spend transactions for this date
        val transactions = transactionService
            .findUserTransactionsForDate(userId, relatedDateId)
            .filter { it.type == TransactionType.SPEND && it.status == TransactionStatus.COMPLETED }

        // 3. Ensure transactions exists, payment received and not refunded already.
        if (transactions.isEmpty()) {
            throw NotFoundException("No completed spend transactions found for date $relatedDateId")
        }

        val purchaseTransaction = transactions.firstOrNull {
            it.status == TransactionStatus.COMPLETED &&
                    it.type == TransactionType.PURCHASE
        } ?: throw ConflictException("No completed payment found for refund")

        val existingRefund = transactions.any {
            it.type == TransactionType.REFUND
        }

        if (existingRefund) {
            throw ConflictException("Refund already processed for this transaction")
        }

        // 4. Calculate refund amount.
        val refundAmount = when (refundPolicy) {
            RefundPolicy.FULL_REFUND -> purchaseTransaction.amount.abs()
            RefundPolicy.PARTIAL_REFUND -> purchaseTransaction.amount.abs() / BigDecimal(2)
            RefundPolicy.NO_REFUND -> BigDecimal.ZERO
        }

        // 5. Create refund transaction if amount > 0
        if (refundAmount > BigDecimal.ZERO) {
            val newBalance = wallet.tokenBalance + refundAmount

            // Create refund transaction
            transactionService.createRefundTransaction(
                userId = userId,
                amount = refundAmount,
                relatedDateId = relatedDateId,
                description = "Refund for cancelled date $relatedDateId (${refundPolicy.name})",
                relatedTransactionId = transactions.last().transactionId,
                newBalance = newBalance,
                metadata = mapOf("refund_policy" to refundPolicy.name)
            )

            // Update wallet
            walletRepository.update(userId, wallet.copy(
                tokenBalance = newBalance,
                lifetimePurchased = wallet.lifetimePurchased + refundAmount,  // Refunds count as purchases
                updatedAt = Instant.now(clock)
            )) ?: throw Exception("Failed to update wallet")
        }

        // 6. Get updated wallet and return response
        val updatedWallet = walletRepository.findById(userId)
            ?: throw NotFoundException("Wallet not found after update")

        val pendingSpends = transactionService.findUserPendingTransactions(userId)
        val pendingBalance = pendingSpends.sumOf { it.amount.abs() }

        return WalletResponse(
            balance = updatedWallet.tokenBalance,
            pendingBalance = pendingBalance,
            lifetimeSpent = updatedWallet.lifetimeSpent,
            lifetimePurchased = updatedWallet.lifetimePurchased,
            currency = updatedWallet.currency
        )
    }

    /**
     * Gets wallet balance with pending commitments calculated.
     *
     * Pending balance includes tokens held for dates with PENDING status.
     */
    suspend fun getBalance(userId: String): WalletWithPending {
        // Get wallet (no lock needed - read-only)
        val wallet = walletRepository.findById(userId)
            ?: throw NotFoundException("Wallet not found for user $userId")

        // Calculate pending balance from pending transactions
        val pendingSpends = transactionService.findUserPendingTransactions(userId)
        val pendingBalance = pendingSpends.sumOf { it.amount }

        // Return WalletResponse with pending balance.
        return WalletWithPending(
            tokenBalance = wallet.tokenBalance,
            pendingTokenBalance = wallet.tokenBalance + pendingBalance,
            lifetimeSpent = wallet.lifetimeSpent,
            lifetimePurchased = wallet.lifetimePurchased,
            currency = wallet.currency
        )
    }
}