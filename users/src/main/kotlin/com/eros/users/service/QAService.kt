package com.eros.users.service

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.users.models.AddUserQARequest
import com.eros.users.models.CreateQuestionRequest
import com.eros.users.models.Question
import com.eros.users.models.QuestionDTO
import com.eros.users.models.UpdateUserQARequest
import com.eros.users.models.UserQAId
import com.eros.users.models.UserQAItem
import com.eros.users.repository.QuestionRepository
import com.eros.users.repository.UserQARepository
import java.time.Clock
import java.time.Instant

class QAService(
    private val questionRepository: QuestionRepository,
    private val userQARepository: UserQARepository,
    private val clock: Clock = Clock.systemUTC()
) {

    // -------------------------------------------------------------------------
    // Question Services
    // -------------------------------------------------------------------------

    /**
     * Admin function to create a new question.
     *
     * @param request [CreateQuestionRequest] containing a question string to be added.
     */
    suspend fun createNewQuestion(request: CreateQuestionRequest): Question {
        val now = Instant.now(clock)
        val question = Question(
            questionId = 0L,
            question = request.question,
            createdAt = now,
            updatedAt = now
        )
        return questionRepository.create(question)
    }


    /**
     * Admin function to update a question.
     */
    suspend fun updateQuestion(request: QuestionDTO): Question? {

        val existing = questionRepository.findById(request.questionId)
            ?: throw NotFoundException("Can't find question with id: ${request.questionId}")
        val updated = Question(
            questionId = request.questionId,
            question = request.question,
            createdAt = existing.createdAt,
            updatedAt = Instant.now(clock)
        )
        return questionRepository.update(request.questionId, updated)
    }

    /**
     * Function to retrieve and return a list of all the questions in the database.
     *
     * @return List of [Question] objects for all the questions in the database.
     */
    suspend fun getAllQuestions(): List<Question> {
        return questionRepository.findAll()
    }

    /**
     * Function for deleting a question from the database.
     *
     * @param questionId id of the question record it be removed.
     *
     * @return The number of rows affected.
     */
    suspend fun deleteQuestion(questionId : Long): Int{
        return questionRepository.delete(questionId)
    }

    // -------------------------------------------------------------------------
    // UserQA Services.
    // -------------------------------------------------------------------------

    suspend fun createUserQA(request: AddUserQARequest): UserQAItem = dbQuery {
        val now = Instant.now(clock)
        val userQA = UserQAItem(
            userId = request.userId,
            question = Question(request.question.questionId, request.question.question, now, now),
            answer = request.answer,
            displayOrder = request.displayOrder,
            createdAt = now,
            updatedAt = now
        )
        userQARepository.create(userQA)
    }

    suspend fun updateUserQA(request: UpdateUserQARequest): UserQAItem = dbQuery {
        val now = Instant.now(clock)
        val updateId = UserQAId(request.userId, request.question.questionId)
        val existing = userQARepository.findById(updateId) ?: throw NotFoundException("User Q&A could not be found.")
        val updates = UserQAItem(
            userId = request.userId,
            question = Question(request.question.questionId,request.question.question,existing.createdAt,existing.updatedAt),
            answer = request.answer ?: existing.answer,
            displayOrder = request.displayOrder ?: existing.displayOrder,
            createdAt = existing.createdAt,
            updatedAt = now
        )
        userQARepository.update(updateId, updates) ?: throw NotFoundException("User Q&A could not be updated.")
    }

    suspend fun getAllQAs(userid : String) : List<UserQAItem> = dbQuery {
        userQARepository.findAllByUserId(userid)
    }

    suspend fun deleteUserQA(userId : String, questionId : Long) : Int = dbQuery {
        userQARepository.delete(UserQAId(userId, questionId))
    }

}