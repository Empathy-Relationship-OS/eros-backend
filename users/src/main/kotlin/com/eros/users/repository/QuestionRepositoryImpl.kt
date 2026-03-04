package com.eros.users.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.users.models.Question
import com.eros.users.table.Questions
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
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
        updatedAt =  Instant.now(clock)
    )

    override fun toStatement(statement: UpdateBuilder<*>, entity: Question) {
        statement.apply {
            this[Questions.question] = entity.question
            this[Questions.createdAt] = entity.createdAt
            this[Questions.updatedAt] = Instant.now(clock)
        }
    }

    /**
     * Finds a question based on an id.
     * @param questionId id of the question to find
     * @return [Question] domain object of the record.
     */
    override suspend fun getQuestionById(questionId: Long) : Question? {
        return table.selectAll()
            .where {Questions.questionId eq questionId}
            .singleOrNull()?.toDomain()
    }


    /**
     * Find if a question is already in the database.
     *
     * @param question String of the question to search for
     * @return `true` if the question is in the database, otherwise `false`
     */
    override suspend fun questionExists(question: String): Boolean {
        return !table.selectAll()
            .where { Questions.question eq question }
            .empty().not()
    }
}