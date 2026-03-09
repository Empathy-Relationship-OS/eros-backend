package com.eros.wallet.models

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Domain Object for a users Wallet.
 * Contains information about users token balance, their lifetime spent and purchase, along with the currency their use.
 *
 */
data class Wallet(
    val userId: String,
    val tokenBalance: Double,
    val lifetimeSpent: Double,
    val lifetimePurchased: Double,
    val currency: String = "GBP",
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun hasSufficientBalance(amount: Double): Boolean = tokenBalance >= amount

    fun pendingBalance(pendingCommitments: List<Double>): Double {
        return pendingCommitments.sum()
    }
}

/**
 * DTO object for a user's wallet.
 */
@Serializable
data class WalletResponse(
    val balance: Double,
    val pendingBalance: Double,
    val lifetimeSpent: Double,
    val lifetimePurchased: Double,
    val currency: String
) {
    companion object {
        fun from(wallet: Wallet, pendingBalance: Double): WalletResponse {
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
    val tokenBalance: Double,
    val pendingTokenBalance: Double,
    val lifetimeSpent: Double,
    val lifetimePurchased: Double,
    val currency: String = "GBP"
)


/**
 * Wallet DTO with a pending balance field
 */
@Serializable
data class WalletWithPendingDTO(
    val tokenBalance: Double,
    val pendingTokenBalance: Double,
    val lifetimeSpent: Double,
    val lifetimePurchased: Double,
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