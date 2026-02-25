package com.eros.users.models

import com.eros.common.serializers.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime

/**
 * User Q&A domain model for question/answer pairs
 */

data class UserQAItem(
    val userId: String,
    val questionId: Long,
    val answer: String,
    val displayOrder: Int, // 1-3
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        require(displayOrder in 1..3) { "Display order must be between 1 and 3" }
        require(answer.isNotBlank()) { "Answer is required" }
        require(answer.length <= 200) { "Answer must not exceed 200 characters" }
    }
}

fun UserQAItem.toDTO() = UserQAItemResponse(
    userId = this.userId,
    questionId = this.questionId,
    answer = this.answer,
    displayOrder = this.displayOrder
)

/**
 * Response DTO for QA item.
 */
@Serializable
data class UserQAItemResponse(
    val userId: String,
    val questionId: Long,
    val answer: String,
    val displayOrder: Int, // 1-3
)



/**
 * Request DTO for adding a Q&A
 */
@Serializable
data class AddUserQARequest(
    val userId: String,
    val questionId: Long,
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
    val userId: String,
    val questionId: Long,
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

/**
 * Response containing all Q&As for a user, ordered by displayOrder
 */
@Serializable
data class UserQACollectionResponse(
    val userId: String,
    val qas: List<UserQAItemResponse>,
    val totalCount: Int
)


/**
 * Request for deleting a single QA record from the database.
 */
@Serializable
data class DeleteUserQARequest(
    val userId: String,
    val questionId: Long,
)



data class UserQAId(
    val userId: String,
    val questionId: Long
) : Comparable<UserQAId> {
    override fun compareTo(other: UserQAId): Int {
        val userIdComparison = userId.compareTo(other.userId)
        return if (userIdComparison != 0) userIdComparison
        else questionId.compareTo(other.questionId)
    }
}