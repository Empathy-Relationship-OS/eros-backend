package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.Question

/**
 * Interface for creating, updating and deleting
 */
interface QuestionRepository : IBaseDAO<Long, Question> {

}