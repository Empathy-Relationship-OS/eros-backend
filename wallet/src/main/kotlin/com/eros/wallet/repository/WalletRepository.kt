package com.eros.wallet.repository

import com.eros.database.repository.IBaseDAO
import com.eros.wallet.models.Wallet
import java.math.BigDecimal

interface WalletRepository : IBaseDAO<String, Wallet> {

    suspend fun updateBalance(userId: String, newBalance: BigDecimal, newLifetimeSpent: BigDecimal) : Wallet?


    suspend fun creditBalance(userId: String, newBalance: BigDecimal) : Wallet?


    suspend fun findByIdForUpdate(userId: String) : Wallet?
}