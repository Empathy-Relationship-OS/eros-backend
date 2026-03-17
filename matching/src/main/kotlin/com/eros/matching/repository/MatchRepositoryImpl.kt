package com.eros.matching.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.matching.models.Match
import com.eros.matching.tables.Matches
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MatchRepositoryImpl : BaseDAOImpl<Long, Match>(Matches, Matches.matchId), MatchRepository {

    // -------------------------------------------------------------------------
    // BaseDAOImpl required implementations
    // -------------------------------------------------------------------------

    override fun ResultRow.toDomain(): Match {
        return Match(
            matchId = this[Matches.matchId],
            user1Id = this[Matches.user1Id],
            user2Id = this[Matches.user2Id],
            liked = this[Matches.liked] ?: false,
            createdAt = this[Matches.createdAt],
            updatedAt = this[Matches.updatedAt],
            servedAt = this[Matches.servedAt]
        )
    }

    override fun toStatement(statement: UpdateBuilder<*>, entity: Match) {
        statement[Matches.user1Id] = entity.user1Id
        statement[Matches.user2Id] = entity.user2Id
        statement[Matches.liked] = entity.liked
        statement[Matches.createdAt] = entity.createdAt
        statement[Matches.updatedAt] = entity.updatedAt
        statement[Matches.servedAt] = entity.servedAt
    }

    // -------------------------------------------------------------------------
    // Match-specific query methods
    // -------------------------------------------------------------------------

    override suspend fun getLikeMatch(fromUserId: String, toUserId: String): Match? {
        return Matches.selectAll()
            .where {
                (Matches.user1Id eq fromUserId) and
                (Matches.user2Id eq toUserId) and
                (Matches.liked eq true)
            }
            .singleOrNull()
            ?.toDomain()
    }

    override suspend fun findUnservedMatches(userId: String, limit: Int): List<Match> {
        return Matches.selectAll()
            .where {
                (Matches.user1Id eq userId) and
                (Matches.servedAt.isNull())
            }
            .limit(limit)
            .map { it.toDomain() }
    }

    override suspend fun markAsServed(matchIds: List<Long>, servedAt: Instant): Int {
        var count = 0
        matchIds.forEach { matchId ->
            val updated = update(matchId,
                findById(matchId)?.copy(servedAt = servedAt)
                    ?: return@forEach
            )
            if (updated != null) count++
        }
        return count
    }

    override suspend fun countServedToday(userId: String, date: LocalDate): Int {
        val startOfDay = date.atStartOfDay(ZoneId.of("UTC")).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant()

        return Matches.selectAll()
            .where {
                (Matches.user1Id eq userId) and
                (Matches.servedAt.isNotNull()) and
                (Matches.servedAt greaterEq startOfDay) and
                (Matches.servedAt less endOfDay)
            }
            .count()
            .toInt()
    }

    override suspend fun findByUserPair(user1Id: String, user2Id: String): Match? {
        return Matches.selectAll()
            .where {
                (Matches.user1Id eq user1Id) and
                (Matches.user2Id eq user2Id)
            }
            .singleOrNull()
            ?.toDomain()
    }
}