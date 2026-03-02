package com.eros.users.table


import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * User Q&A table for storing question/answer pairs
 * 
 * Requirements:
 * - Minimum 1 Q&A per user
 * - Maximum 3 Q&As per user
 * - Questions are predefined (PredefinedQuestion enum)
 * - Answers max 200 characters
 * - User-defined ordering (displayOrder 1-3)
 */
object UserQA : Table("user_qa") {

    // Foreign key to Users table
    val userId = varchar("user_id", 128).references(Users.userId)
    
    // Q&A content
    val questionId = long("question_id").references(Questions.questionId) // Index of the question
    val answer = varchar("answer", 200) // User's answer, max 200 chars
    
    // Ordering
    val displayOrder = integer("display_order") // 1-3, user-defined order
    
    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    // Set primary key.
    override val primaryKey = PrimaryKey(userId, questionId)

    init {
        // Ensure displayOrder is unique per user
        uniqueIndex("unique_qa_display_order_per_user", userId, displayOrder)

        // Check constraint: displayOrder must be between 1 and 3
        check("qa_display_order_range") { displayOrder.between(1, 3) }
    }
}
