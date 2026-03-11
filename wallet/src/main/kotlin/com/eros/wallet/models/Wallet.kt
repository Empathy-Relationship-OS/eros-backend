package com.eros.wallet.models

import com.eros.wallet.table.Wallets
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import java.math.BigDecimal
import java.time.Instant

/**
 * Domain Object for a users Wallet.
 * Contains information about users token balance, their lifetime spent and purchase, along with the currency their use.
 *
 */
data class Wallet(
    val userId: String,
    val tokenBalance: BigDecimal,
    val lifetimeSpent: BigDecimal,
    val lifetimePurchased: BigDecimal,
    val currency: String = "GBP",
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun hasSufficientBalance(amount: BigDecimal): Boolean = tokenBalance >= amount

    fun pendingBalance(pendingCommitments: List<BigDecimal>): BigDecimal {
        return pendingCommitments.fold(BigDecimal.ZERO, BigDecimal::add)
    }
}

/**
 * DTO object for a user's wallet.
 */
@Serializable
data class WalletResponse(
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val pendingBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val lifetimeSpent: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val lifetimePurchased: BigDecimal,
    val currency: String
) {
    companion object {
        fun from(wallet: Wallet, pendingBalance: BigDecimal): WalletResponse {
            return WalletResponse(
                balance = wallet.tokenBalance,
                pendingBalance = pendingBalance,
                lifetimeSpent = wallet.lifetimeSpent,
                lifetimePurchased = wallet.lifetimePurchased,
                currency = wallet.currency
            )
        }
    }
}

/**
 * Wallet with a pending balance field
 */
data class WalletWithPending(
    val tokenBalance: BigDecimal,
    val pendingTokenBalance: BigDecimal,
    val lifetimeSpent: BigDecimal,
    val lifetimePurchased: BigDecimal,
    val currency: String = "GBP"
)

/**
 * Function for converting the wallet with pending to a DTO.
 */
fun WalletWithPending.toDTO() = WalletResponse(
    balance = this.tokenBalance,
    pendingBalance = this.pendingTokenBalance,
    lifetimeSpent = this.lifetimeSpent,
    lifetimePurchased = this.lifetimePurchased,
    currency = this.currency
)

fun ResultRow.toWalletDomain(): Wallet {
    return Wallet(
        userId = this[Wallets.userId],
        tokenBalance = this[Wallets.tokenBalance],
        lifetimeSpent = this[Wallets.lifetimeSpent],
        lifetimePurchased = this[Wallets.lifetimePurchased],
        currency = this[Wallets.currency],
        createdAt = this[Wallets.createdAt],
        updatedAt = this[Wallets.updatedAt]
    )
}



/*
/**
 * Extension function to convert a Domain object to a DTO object.
 */
fun Wallet.toDTO() = WalletResponse(
    tokenBalance = tokenBalance,
    lifetimeSpent = lifetimeSpent,
    lifetimePurchased = lifetimePurchased,
    currency = currency
)


/**
 * Wallet with a pending balance field
 */
data class WalletWithPending(
    val tokenBalance: BigDecimal,
    val pendingTokenBalance: BigDecimal,
    val lifetimeSpent: BigDecimal,
    val lifetimePurchased: BigDecimal,
    val currency: String = "GBP"
)


/**
 * Wallet DTO with a pending balance field
 */
@Serializable
data class WalletWithPendingDTO(
    val tokenBalance: BigDecimal,
    val pendingTokenBalance: BigDecimal,
    val lifetimeSpent: BigDecimal,
    val lifetimePurchased: BigDecimal,
    val currency: String = "GBP"
)

/**
 * Helper Function to convert a WalletWithPending domain object to a DTO.
 */
fun WalletWithPending.toDTO() = WalletWithPendingDTO(
    tokenBalance = tokenBalance,
    pendingTokenBalance = pendingTokenBalance,
    lifetimeSpent = lifetimeSpent,
    lifetimePurchased = lifetimePurchased,
    currency = currency
)

 */