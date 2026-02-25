package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.database.repository.CompositeKeyDAOImpl
import com.eros.users.models.UserQAId
import com.eros.users.models.UserQAItem
import com.eros.users.table.UserQA
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
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
        questionId = this[UserQA.questionId],
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
            this[UserQA.questionId] = entity.questionId
            this[UserQA.userId] = entity.userId
            this[UserQA.answer] = entity.answer
            this[UserQA.createdAt] = entity.createdAt
            this[UserQA.updatedAt] = entity.updatedAt
            this[UserQA.displayOrder] = entity.displayOrder
            this[UserQA.updatedAt] = entity.updatedAt
            this[UserQA.createdAt] = entity.createdAt
        }
    }

    override suspend fun findAllByUserId(userId : String): List<UserQAItem>{
        return table.selectAll()
            .where {idColumn1 eq userId }
            .map {it.toDomain()}
    }
}