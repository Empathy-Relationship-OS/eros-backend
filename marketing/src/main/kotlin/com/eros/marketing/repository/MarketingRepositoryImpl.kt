package com.eros.marketing.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.marketing.models.UserMarketingConsent
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import com.eros.marketing.tables.UserMarketingConsent as UserMarketingConsentTable

class MarketingRepositoryImpl(
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
        statement[UserMarketingConsentTable.updatedAt] = entity.updatedAt
    }

    // -------------------------------------------------------------------------
    // Override create to explicitly set createdAt during INSERT
    // -------------------------------------------------------------------------

    override suspend fun create(entity: UserMarketingConsent): UserMarketingConsent {
        return UserMarketingConsentTable.insertReturning {
            toStatement(it, entity)
            it[UserMarketingConsentTable.createdAt] = entity.createdAt
        }.single().toDomain()
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

    /**
     * Atomically upserts (insert or update) a marketing consent record.
     *
     * Uses PostgreSQL's ON CONFLICT clause to handle the race condition between
     * checking existence and inserting/updating. This ensures atomicity at the
     * database level.
     *
     * @param entity The marketing consent entity to upsert
     * @return The upserted marketing consent record
     */
    override suspend fun upsert(entity: UserMarketingConsent): UserMarketingConsent {
        UserMarketingConsentTable.upsert(
            keys = arrayOf(UserMarketingConsentTable.userId),
            onUpdate = { update ->
                update[UserMarketingConsentTable.marketingConsent] = entity.marketingConsent
                update[UserMarketingConsentTable.updatedAt] = entity.updatedAt
            }
        ) {
            it[UserMarketingConsentTable.userId] = entity.userId
            it[UserMarketingConsentTable.marketingConsent] = entity.marketingConsent
            it[UserMarketingConsentTable.createdAt] = entity.createdAt
            it[UserMarketingConsentTable.updatedAt] = entity.updatedAt
        }

        // Fetch and return the upserted record
        return findById(entity.userId)
            ?: error("Upserted record not found for userId: ${entity.userId}")
    }
}
