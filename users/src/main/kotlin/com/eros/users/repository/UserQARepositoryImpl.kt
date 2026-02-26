package com.eros.users.repository

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.database.repository.CompositeKeyDAOImpl
import com.eros.users.models.Question
import com.eros.users.models.UserQAId
import com.eros.users.models.UserQAItem
import com.eros.users.table.Questions
import com.eros.users.table.UserQA
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock

class UserQARepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
): CompositeKeyDAOImpl<String, Long, UserQAId, UserQAItem>(
    table = UserQA,
    idColumn1 = UserQA.userId,
    idColumn2 = UserQA.questionId
), UserQARepository {
    override fun UserQAId.getKey1(): String = userId
    override fun UserQAId.getKey2(): Long = questionId

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
            this[UserQA.createdAt] = entity.createdAt
            this[UserQA.updatedAt] = entity.updatedAt
            this[UserQA.displayOrder] = entity.displayOrder
            this[UserQA.updatedAt] = entity.updatedAt
            this[UserQA.createdAt] = entity.createdAt
        }
    }


    override fun findAllByUserId(userId: String): List<UserQAItem> {
        return (UserQA innerJoin Questions)
            .selectAll()
            .where { UserQA.userId eq userId }
            .orderBy(UserQA.displayOrder)
            .map { it.toDomain() }
    }


    override fun findById(id: UserQAId): UserQAItem? {
        return (UserQA innerJoin Questions)
            .selectAll()
            .where {
                (UserQA.userId eq id.userId) and (UserQA.questionId eq id.questionId)
            }
            .firstOrNull()
            ?.toDomain()
    }


    override fun findAll(): List<UserQAItem> {
        return (UserQA innerJoin Questions)
            .selectAll()
            .map { it.toDomain() }
    }


    override fun create(entity: UserQAItem): UserQAItem {
        UserQA.insert { toStatement(it, entity) }
        return findById(UserQAId(entity.userId, entity.question.questionId)) ?: throw NotFoundException("Can't find the Q&A")
    }
}