package com.eros.wallet.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.wallet.models.Wallet
import com.eros.wallet.models.toWalletDomain
import com.eros.wallet.table.Wallets
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.math.BigDecimal
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
            this[Wallets.walletId] = entity.walletId
            this[Wallets.userId] = entity.userId
            this[Wallets.tokenBalance] = entity.tokenBalance
            this[Wallets.lifetimeSpent] = entity.lifetimeSpent
            this[Wallets.lifetimePurchased] = entity.lifetimePurchased
            this[Wallets.currency] = entity.currency
            this[Wallets.createdAt] = entity.createdAt
            this[Wallets.updatedAt] = entity.updatedAt
        }
    }


    /**
     * Function to update the balance of a user's wallet.
     */
    override suspend fun creditBalance(userId: String, newBalance: BigDecimal) : Wallet? {
        return Wallets.updateReturning(where = { Wallets.userId eq userId }) {
            it[tokenBalance] = newBalance
            it[updatedAt] = Instant.now(clock)
        }.singleOrNull()?.toDomain()
    }


    /**
     * Function to update the balance of a user's wallet.
     */
    override suspend fun updateBalance(userId: String, newBalance: BigDecimal, newLifetimeSpent: BigDecimal) : Wallet? {
        return Wallets.updateReturning(where = { Wallets.userId eq userId }) {
            it[tokenBalance] = newBalance
            it[lifetimeSpent] = newLifetimeSpent
            it[updatedAt] = Instant.now(clock)
        }.singleOrNull()?.toDomain()
    }


    /**
     * Function to retrieve the wallet for updating.
     *
     * This includes row-level locking for concurrent updates. Any transaction/dbQuery that retrieves a wallet will
     * lock that record for the duration of the transaction/dbQuery.
     *
     * (Use findById for general checking of balance - May be outdated if update function is updating their balance)
     *
     * @return [Wallet] if the user has a wallet, otherwise `null`.
     */
    override suspend fun findByIdForUpdate(userId: String): Wallet? {
        return Wallets.selectAll()
            .where { Wallets.userId eq userId }
            .forUpdate()
            .singleOrNull()?.toDomain()
    }
}