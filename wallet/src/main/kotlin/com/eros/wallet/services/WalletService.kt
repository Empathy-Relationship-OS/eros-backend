package com.eros.wallet.services

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.wallet.models.Transaction
import com.eros.wallet.models.TransactionStatus
import com.eros.wallet.models.TransactionType
import com.eros.wallet.models.Wallet
import com.eros.wallet.repository.TransactionRepository
import com.eros.wallet.repository.WalletRepository
import java.time.Clock
import java.time.Instant

class WalletService(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
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


    suspend fun createPurchase() : Unit = dbQuery { throw NotImplementedError() }


    suspend fun spendTokens() : Unit = dbQuery { throw NotImplementedError() }


    suspend fun refundTokens() : Unit = dbQuery { throw NotImplementedError() }


    suspend fun getBalance() : Unit = dbQuery { throw NotImplementedError() }


    suspend fun getTransactionHistory() : Unit = dbQuery { throw NotImplementedError() }
}