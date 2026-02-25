package com.eros.users.models

import kotlinx.serialization.Serializable
import java.time.Instant

data class Question(
    val questionId : Long,
    val question : String,
    val createdAt : Instant,
    val updatedAt : Instant
)

/**
 * Admin only request for adding a new question to the list.
 */
@Serializable
data class CreateQuestionRequest(
    val question : String
)

/**
 * Admin only response for adding a new question to the list.
 */
@Serializable
data class QuestionResponse(
    val questionId : Long,
    val question : String
)


/**
 * Admin only request for updating the question with the provided id to the provided question string.
 */
@Serializable
data class UpdateQuestionRequest(
    val questionId : Long,
    val question : String
)

fun Question.toDTO() = QuestionResponse(
    questionId = this.questionId,
    question = this.question
)
