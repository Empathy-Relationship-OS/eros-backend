package com.eros.matching.repository

import com.eros.matching.models.DailyBatch
import com.eros.matching.tables.UserDailyBatches
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class DailyBatchRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : DailyBatchRepository {

    override suspend fun findByUserAndDate(userId: String, date: LocalDate): DailyBatch? {
        return UserDailyBatches.selectAll()
            .where {
                (UserDailyBatches.userId eq userId) and
                (UserDailyBatches.batchDate eq date)
            }
            .singleOrNull()
            ?.let {
                DailyBatch(
                    userId = it[UserDailyBatches.userId],
                    batchDate = it[UserDailyBatches.batchDate],
                    batchCount = it[UserDailyBatches.batchCount],
                    createdAt = it[UserDailyBatches.createdAt],
                    updatedAt = it[UserDailyBatches.updatedAt]
                )
            }
    }

    override suspend fun create(dailyBatch: DailyBatch): DailyBatch {
        return UserDailyBatches.insertReturning {
            it[userId] = dailyBatch.userId
            it[batchDate] = dailyBatch.batchDate
            it[batchCount] = dailyBatch.batchCount
            it[createdAt] = dailyBatch.createdAt
            it[updatedAt] = dailyBatch.updatedAt
        }.single().let {
            DailyBatch(
                userId = it[UserDailyBatches.userId],
                batchDate = it[UserDailyBatches.batchDate],
                batchCount = it[UserDailyBatches.batchCount],
                createdAt = it[UserDailyBatches.createdAt],
                updatedAt = it[UserDailyBatches.updatedAt]
            )
        }
    }

    override suspend fun update(dailyBatch: DailyBatch): DailyBatch? {
        return UserDailyBatches.updateReturning(
            where = {
                (UserDailyBatches.userId eq dailyBatch.userId) and
                (UserDailyBatches.batchDate eq dailyBatch.batchDate)
            },
            body = {
                it[batchCount] = dailyBatch.batchCount
                it[updatedAt] = dailyBatch.updatedAt
            }
        ).singleOrNull()?.let {
            DailyBatch(
                userId = it[UserDailyBatches.userId],
                batchDate = it[UserDailyBatches.batchDate],
                batchCount = it[UserDailyBatches.batchCount],
                createdAt = it[UserDailyBatches.createdAt],
                updatedAt = it[UserDailyBatches.updatedAt]
            )
        }
    }

    override suspend fun incrementBatchCount(userId: String, date: LocalDate): DailyBatch {
        val existing = findByUserAndDate(userId, date)

        return if (existing != null) {
            val incremented = existing.incrementBatchCount()
            update(incremented) ?: throw IllegalStateException("Failed to update batch count for user $userId on $date")
        } else {
            val now = Instant.now(clock)
            create(
                DailyBatch(
                    userId = userId,
                    batchDate = date,
                    batchCount = 1,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    override suspend fun getBatchCount(userId: String, date: LocalDate): Int {
        return findByUserAndDate(userId, date)?.batchCount ?: 0
    }
}
