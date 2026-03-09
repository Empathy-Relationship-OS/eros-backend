package com.eros.users.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Questions : Table("questions") {

    // Primary key.
    val questionId = long("id").autoIncrement()

    // Question
    val question = varchar("question", 100)

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(questionId)

    init {
        // Ensure question is unique
        uniqueIndex("unique_question", question)
    }

}

