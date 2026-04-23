package com.eros.marketing.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.marketing.models.UserMarketingConsent
import com.eros.marketing.tables.UserMarketingConsent as UserMarketingConsentTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class MarketingRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<String, UserMarketingConsent>(UserMarketingConsentTable, UserMarketingConsentTable.userId), MarketingRepository {

    // -------------------------------------------------------------------------
    // BaseDAOImpl required implementations
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): UserMarketingConsent {
        return UserMarketingConsent(
            userId = this[UserMarketingConsentTable.userId],
            marketingConsent = this[UserMarketingConsentTable.marketingConsent],
            createdAt = this[UserMarketingConsentTable.createdAt],
            updatedAt = this[UserMarketingConsentTable.updatedAt]
        )
    }

    override fun toStatement(statement: UpdateBuilder<*>, entity: UserMarketingConsent) {
        statement[UserMarketingConsentTable.userId] = entity.userId
        statement[UserMarketingConsentTable.marketingConsent] = entity.marketingConsent
        statement[UserMarketingConsentTable.createdAt] = entity.createdAt
        statement[UserMarketingConsentTable.updatedAt] = entity.updatedAt
    }

    // -------------------------------------------------------------------------
    // Marketing-specific query methods
    // -------------------------------------------------------------------------

    override suspend fun findAllConsented(): List<UserMarketingConsent> {
        return UserMarketingConsentTable.selectAll()
            .where { UserMarketingConsentTable.marketingConsent eq true }
            .map { it.toDomain() }
    }

    override suspend fun countConsented(): Long {
        return UserMarketingConsentTable.selectAll()
            .where { UserMarketingConsentTable.marketingConsent eq true }
            .count()
    }
}
