package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.Question
import com.eros.users.table.Questions
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Clock
import java.time.Instant

class QuestionRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<Long, Question>(Questions, Questions.questionId), QuestionRepository {

    override fun ResultRow.toDomain() = Question(
        questionId = this[Questions.questionId],
        question = this[Questions.question],
        createdAt = this[Questions.createdAt],
        updatedAt = this[Questions.updatedAt]
    )

    override fun toStatement(statement: UpdateBuilder<*>, entity: Question) {
        statement.apply {
            this[Questions.questionId] = entity.questionId
            this[Questions.question] = entity.question
            this[Questions.createdAt] = entity.createdAt
            this[Questions.updatedAt] = Instant.now(clock)
        }
    }

    override fun getQuestionById(questionId: Long) : Question? {
        return table.selectAll()
            .where {Questions.questionId eq questionId}
            .singleOrNull()?.toDomain()
    }

}