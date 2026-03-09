package com.eros.wallet.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.wallet.models.Wallet
import com.eros.wallet.models.toWalletDomain
import com.eros.wallet.table.Wallets
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.time.Clock
import java.time.Instant


class WalletRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<String, Wallet>(Wallets, Wallets.userId), WalletRepository {

    override fun ResultRow.toDomain(): Wallet = toWalletDomain()

    override fun toStatement(
        statement: UpdateBuilder<*>,
        entity: Wallet
    ) {
        statement.apply {
            this[Wallets.userId] = entity.userId
            this[Wallets.tokenBalance] = entity.tokenBalance.toBigDecimal()
            this[Wallets.lifetimeSpent] = entity.lifetimeSpent.toBigDecimal()
            this[Wallets.lifetimePurchased] = entity.lifetimePurchased.toBigDecimal()
            this[Wallets.currency] = entity.currency
            this[Wallets.createdAt] = entity.createdAt
            this[Wallets.updatedAt] = entity.updatedAt
        }
    }


    /**
     * Function to update the balance of a user's wallet.
     */
    override suspend fun updateBalance(userId: String, newBalance: Double) : Wallet? {
        return Wallets.updateReturning(where = { Wallets.userId eq userId }) {
            it[tokenBalance] = newBalance.toBigDecimal()
            it[updatedAt] = Instant.now(clock)
        }.singleOrNull()?.toDomain()
    }
}