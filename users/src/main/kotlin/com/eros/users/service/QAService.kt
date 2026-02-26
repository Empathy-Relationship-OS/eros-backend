package com.eros.users.service

import com.eros.common.errors.BadRequestException
import com.eros.common.errors.ConflictException
import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.users.models.AddUserQARequest
import com.eros.users.models.CreateQuestionRequest
import com.eros.users.models.Question
import com.eros.users.models.QuestionDTO
import com.eros.users.models.UpdateUserQARequest
import com.eros.users.models.UserQACollection
import com.eros.users.models.UserQACollectionDTO
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
     * @throws ConflictException if the question already exists.
     */
    suspend fun createNewQuestion(request: CreateQuestionRequest): Question {
        // Ensure that the question is not in the table.
        val exists = questionRepository.questionExists(request.question)
        if (exists) throw ConflictException("Question already exists.")

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
     *
     * @param QuestionDTO The id of the question with the updated question.
     * @throws NotFoundException if the id is not in the database.
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
     * @return [Int] The number of rows deleted (1 or 0).
     */
    suspend fun deleteQuestion(questionId : Long): Int{
        return questionRepository.delete(questionId)
    }

    // -------------------------------------------------------------------------
    // UserQA Services.
    // -------------------------------------------------------------------------

    /**
     * Function to create a single userQA item - linking their answer to a question.
     *
     * @param request Request DTO that will be created.
     *
     * @return [UserQAItem] with the information provided in the request.
     * @throws BadRequestException if the user already had 3 QAs answered.
     * @throws ConflictException if the user has already answered this question.
     */
    suspend fun createUserQA(request: AddUserQARequest): UserQAItem = dbQuery {
        val now = Instant.now(clock)

        // Ensure the user can't add more than 3 Q&A.
        val currentCount = userQARepository.findAllByUserId(request.userId).size
        if (currentCount == 3) {
            throw BadRequestException("Maximum of 3 Q&A's allowed per user.")
        }

        // Ensure the user hasn't already answered this question.
        val exists = userQARepository.doesExist(UserQAId(request.userId,request.question.questionId))
        if (exists) {
            throw ConflictException("User already has answered this question.")
        }

        // Create UserQAObject and create it in the database.
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

    /**
     * Function to update a user QA record.
     *
     * @param request - UpdateUserQARequest containing userId, questionId and either one of or both of the
     *                  answer and displayOrder.
     *
     * @return [UserQAItem] the updated QA record as a domain object.
     * @throws NotFoundException if the QA record can't be found.
     */
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

    /**
     * Function to update / add a whole UserQACollection.
     *
     * @param request UserQACollectionResponse DTO that contains information about all the QAs to be added.
     */
    suspend fun createUserQACollection(request : UserQACollectionDTO) : UserQACollection = dbQuery{
        val now = Instant.now(clock)

        // Delete all existing
        for (i in 0..request.totalCount-1){
            userQARepository.deleteAllByUserId(request.qas[i].userId)
        }

        // Add new collection
        val qas = emptyList<UserQAItem>()
        for (i in 0..request.totalCount-1){
            val userQA = UserQAItem(
                userId = request.userId,
                question = Question(request.qas[i].question.questionId, request.qas[i].question.question, now, now),
                answer = request.qas[i].answer,
                displayOrder = request.qas[i].displayOrder,
                createdAt = now,
                updatedAt = now
            )
            qas.plus(userQARepository.create(userQA))
        }
        UserQACollection(request.userId, qas, qas.count())
    }


    /**
     * Function to return a list of all the QA records for a user.
     *
     * @param userId The userId of the user to retrieve the records for.
     * @return [List] of [UserQAItem] retrieved for that user.
     */
    suspend fun getAllUserQAs(userId : String) : List<UserQAItem> = dbQuery {
        userQARepository.findAllByUserId(userId)
    }


    /**
     * Delete a single record for a user's QA.
     *
     * @param userId id of the user.
     * @param questionId id of the question that is being removed from their QA.
     * @return [Int] The number of rows deleted (1 or 0).
     */
    suspend fun deleteUserQA(userId : String, questionId : Long) : Int = dbQuery {
        userQARepository.delete(UserQAId(userId, questionId))
    }

}