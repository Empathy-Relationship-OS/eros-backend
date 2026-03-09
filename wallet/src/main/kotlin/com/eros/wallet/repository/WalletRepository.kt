package com.eros.wallet.repository

import com.eros.database.repository.IBaseDAO
import com.eros.wallet.models.Wallet

interface WalletRepository : IBaseDAO<String, Wallet> {

    suspend fun updateBalance(userId: String, newBalance: Double) : Wallet?
}