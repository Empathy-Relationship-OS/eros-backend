package com.eros.users.repository

import com.eros.common.errors.NotFoundException
import com.eros.database.repository.CompositeKeyDAOImpl
import com.eros.users.models.Question
import com.eros.users.models.UserQAId
import com.eros.users.models.UserQAItem
import com.eros.users.table.Questions
import com.eros.users.table.UserQA
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.updateReturning
import java.time.Clock
import java.time.Instant

/**
 * Repository implementation for UserQA table with composite primary key (userId, questionId).
 *
 * Extends [CompositeKeyDAOImpl] to provide base CRUD operations, and adds custom
 * query methods specific to user Q&A functionality.
 *
 * @param clock Clock instance for timestamp management (injectable for testing).
 */
class UserQARepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : CompositeKeyDAOImpl<UserQAId, UserQAItem>(
    table = UserQA
), UserQARepository {

    /**
     * Builds the WHERE clause for the composite key (userId, questionId).
     */
    override fun buildKeyCondition(id: UserQAId): Op<Boolean> {
        return (UserQA.userId eq id.userId) and (UserQA.questionId eq id.questionId)
    }

    /**
     * Maps a database row to UserQAItem domain model.
     *
     * NOTE: This expects Questions table to be joined. For base CRUD operations,
     * consider overriding methods that need the join.
     */
    override fun ResultRow.toDomain() = UserQAItem(
        question = Question(
            questionId = this[Questions.questionId],
            question = this[Questions.question],
            createdAt = this[Questions.createdAt],
            updatedAt = this[Questions.updatedAt]
        ),
        userId = this[UserQA.userId],
        createdAt = this[UserQA.createdAt],
        updatedAt = this[UserQA.updatedAt],
        displayOrder = this[UserQA.displayOrder],
        answer = this[UserQA.answer]
    )

    override fun toStatement(
        statement: UpdateBuilder<*>,
        entity: UserQAItem
    ) {
        statement.apply {
            this[UserQA.questionId] = entity.question.questionId
            this[UserQA.userId] = entity.userId
            this[UserQA.answer] = entity.answer
            this[UserQA.displayOrder] = entity.displayOrder
            this[UserQA.updatedAt] = Instant.now(clock)
            this[UserQA.createdAt] = entity.createdAt
        }
    }

    // -------------------------------------------------------------------------
    // Custom UserQA-specific methods
    // -------------------------------------------------------------------------
    // NOTE: These methods do NOT wrap calls in dbQuery/transaction blocks.
    // The SERVICE layer manages transaction boundaries using dbQuerySuspend { }.
    // These are NOT suspend because Exposed v1 uses blocking I/O.
    // -------------------------------------------------------------------------

    /**
     * Find all user QA records using a userId.
     *
     * @param userId id of the user to search for
     * @return List of [UserQAItem] for the provided user, ordered by displayOrder.
     */
    override suspend fun findAllByUserId(userId: String): List<UserQAItem> {
        return (UserQA innerJoin Questions)
            .selectAll()
            .where { UserQA.userId eq userId }
            .orderBy(UserQA.displayOrder)
            .map { it.toDomain() }
    }

    /**
     * Delete all user QA records using a userId.
     *
     * @param userId id of the user to search for
     * @return Integer of the number of records deleted.
     */
    override suspend fun deleteAllByUserId(userId: String): Int {
        return UserQA.deleteWhere { UserQA.userId eq userId }
    }

    // -------------------------------------------------------------------------
    // Override base methods to add Questions join
    // -------------------------------------------------------------------------

    /**
     * Override to add Questions join for complete Question object.
     */
    override suspend fun findById(id: UserQAId): UserQAItem? {
        return (UserQA innerJoin Questions)
            .selectAll()
            .where { buildKeyCondition(id) }
            .firstOrNull()
            ?.toDomain()
    }

    /**
     * Override to add Questions join for complete Question objects.
     */
    override suspend fun findAll(): List<UserQAItem> {
        return (UserQA innerJoin Questions)
            .selectAll()
            .map { it.toDomain() }
    }

    /**
     * Override to add Questions join after insert.
     */
    override suspend fun create(entity: UserQAItem): UserQAItem {
        UserQA.insert { toStatement(it, entity) }
        return findById(UserQAId(entity.userId, entity.question.questionId))
            ?: throw NotFoundException("Can't find the Q&A after creation")
    }

    /**
     * Override to add Questions join after update.
     */
    override suspend fun update(id: UserQAId, entity: UserQAItem): UserQAItem? {
        super.update(id, entity)
        return findById(id)
    }
}