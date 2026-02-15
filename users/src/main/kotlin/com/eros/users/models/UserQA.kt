package com.eros.users.models

import com.eros.common.serializers.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * User Q&A domain model for question/answer pairs
 */
@Serializable
data class UserQAItem(
    val id: Long,
    val userId: String,
    val question: PredefinedQuestion,
    val answer: String,
    val displayOrder: Int, // 1-3
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime
) {
    init {
        require(displayOrder in 1..3) { "Display order must be between 1 and 3" }
        require(answer.isNotBlank()) { "Answer is required" }
        require(answer.length <= 200) { "Answer must not exceed 200 characters" }
    }
    
    /**
     * Get the display text for the question
     */
    fun getQuestionText(): String = question.getDisplayText()
}

/**
 * Request DTO for adding a Q&A
 */
@Serializable
data class AddUserQARequest(
    val userId: Long,
    val question: PredefinedQuestion,
    val answer: String,
    val displayOrder: Int
) {
    init {
        require(displayOrder in 1..3) { "Display order must be between 1 and 3" }
        require(answer.isNotBlank()) { "Answer is required" }
        require(answer.length <= 200) { "Answer must not exceed 200 characters" }
    }
}

/**
 * Request DTO for updating a Q&A
 */
@Serializable
data class UpdateUserQARequest(
    val question: PredefinedQuestion? = null,
    val answer: String? = null,
    val displayOrder: Int? = null
) {
    init {
        if (displayOrder != null) {
            require(displayOrder in 1..3) { "Display order must be between 1 and 3" }
        }
        if (answer != null) {
            require(answer.isNotBlank()) { "Answer is required" }
            require(answer.length <= 200) { "Answer must not exceed 200 characters" }
        }
    }
}

/**
 * Response containing all Q&As for a user, ordered by displayOrder
 */
@Serializable
data class UserQACollection(
    val userId: String,
    val qas: List<UserQAItem>,
    val totalCount: Int
) {
    /**
     * Check if user has minimum required Q&As (1)
     */
    fun hasMinimumQAs(): Boolean = totalCount >= 1
    
    /**
     * Check if user has reached maximum Q&As (3)
     */
    fun hasReachedMaximum(): Boolean = totalCount >= 3
    
    /**
     * Check if collection is valid (1-3 items)
     */
    fun isValidCollection(): Boolean = totalCount in 1..3
}
