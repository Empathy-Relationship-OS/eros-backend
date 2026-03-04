package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.Question

/**
 * Repository interface for Question entity CRUD operations.
 */
interface QuestionRepository : IBaseDAO<Long, Question> {

    /**
     * Finds a question based on an id.
     * @param questionId id of the question to find
     * @return [Question] domain object of the record.
     */
    fun getQuestionById(questionId : Long) : Question?

    /**
     * Find if a question is already in the database.
     *
     * @param question String of the question to search for
     * @return `true` if the question is in the database, otherwise `false`
     */
    fun questionExists(question : String) : Boolean
}