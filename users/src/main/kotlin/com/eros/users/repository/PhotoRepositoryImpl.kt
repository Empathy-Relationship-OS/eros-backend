package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.MediaType
import com.eros.users.models.UserMediaItem
import com.eros.users.table.UserMedia
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime

/**
 * Exposed ORM implementation of [PhotoRepository].
 *
 * All writes that need to maintain consistency across multiple rows (e.g. [setPrimary])
 * are executed inside a single [dbQuery] block so Exposed wraps them in one transaction.
 *
 * Timestamps come back from Postgres as [OffsetDateTime] (TIMESTAMPTZ column) and are
 * normalised to UTC [java.time.LocalDateTime] for the domain model.
 *
 * @param clock Injectable clock for consistent timestamp generation in tests.
 */
class PhotoRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : PhotoRepository {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private fun ResultRow.toMediaItem(): UserMediaItem = UserMediaItem(
        id           = this[UserMedia.id],
        userId       = this[UserMedia.userId],
        mediaUrl     = this[UserMedia.mediaUrl],
        thumbnailUrl = this[UserMedia.thumbnailUrl],
        mediaType    = MediaType.valueOf(this[UserMedia.mediaType]),
        displayOrder = this[UserMedia.displayOrder],
        isPrimary    = this[UserMedia.isPrimary],
        createdAt    = this[UserMedia.createdAt],
        updatedAt    = this[UserMedia.updatedAt]
    )

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    override suspend fun findByUserId(userId: String): List<UserMediaItem> = dbQuery {
        UserMedia.selectAll()
            .where { UserMedia.userId eq userId }
            .orderBy(UserMedia.displayOrder)
            .map { it.toMediaItem() }
    }

    override suspend fun findById(id: Long): UserMediaItem? = dbQuery {
        UserMedia.selectAll()
            .where { UserMedia.id eq id }
            .singleOrNull()
            ?.toMediaItem()
    }

    override suspend fun findByDisplayOrder(userId: String, displayOrder: Int): UserMediaItem? = dbQuery {
        UserMedia.selectAll()
            .where { (UserMedia.userId eq userId) and (UserMedia.displayOrder eq displayOrder) }
            .singleOrNull()
            ?.toMediaItem()
    }

    override suspend fun countByUserId(userId: String): Int = dbQuery {
        UserMedia.selectAll()
            .where { UserMedia.userId eq userId }
            .count()
            .toInt()
    }

    // -------------------------------------------------------------------------
    // Writes
    // -------------------------------------------------------------------------

    override suspend fun insert(
        userId: String,
        mediaUrl: String,
        mediaType: String,
        displayOrder: Int,
        isPrimary: Boolean
    ): UserMediaItem = dbQuery {
        val now = Instant.now(clock)
        val id = UserMedia.insert {
            it[UserMedia.userId]       = userId
            it[UserMedia.mediaUrl]     = mediaUrl
            it[UserMedia.mediaType]    = mediaType
            it[UserMedia.displayOrder] = displayOrder
            it[UserMedia.isPrimary]    = isPrimary
            it[UserMedia.createdAt]    = now
            it[UserMedia.updatedAt]    = now
        } get UserMedia.id

        UserMedia.selectAll()
            .where { UserMedia.id eq id }
            .single()
            .toMediaItem()
    }

    override suspend fun updateThumbnailUrl(id: Long, thumbnailUrl: String): UserMediaItem? = dbQuery {
        val rows = UserMedia.update({ UserMedia.id eq id }) {
            it[UserMedia.thumbnailUrl] = thumbnailUrl
            it[UserMedia.updatedAt]    = Instant.now(clock)
        }
        if (rows == 0) null
        else UserMedia.selectAll()
            .where { UserMedia.id eq id }
            .singleOrNull()
            ?.toMediaItem()
    }

    override suspend fun setPrimary(userId: String, id: Long): UserMediaItem? = dbQuery {
        // Clear isPrimary for all rows belonging to this user
        UserMedia.update({ UserMedia.userId eq userId }) {
            it[UserMedia.isPrimary] = false
            it[UserMedia.updatedAt] = Instant.now(clock)
        }
        // Set isPrimary = true only for the target row
        val rows = UserMedia.update({
            (UserMedia.id eq id) and (UserMedia.userId eq userId)
        }) {
            it[UserMedia.isPrimary] = true
            it[UserMedia.updatedAt] = Instant.now(clock)
        }

        if (rows == 0) null
        else UserMedia.selectAll()
            .where { UserMedia.id eq id }
            .singleOrNull()
            ?.toMediaItem()
    }

    override suspend fun deleteById(id: Long): Int = dbQuery {
        UserMedia.deleteWhere { UserMedia.id eq id }
    }
}
