package com.eros.auth.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Users table - Core identity linked to Firebase Auth
 *
 * This table stores minimal user identity data synced with Firebase Authentication.
 * Firebase handles: passwords, OTP verification, email/phone verification, JWT tokens
 * Backend handles: user profile data, business logic, Firebase UID linking
 *
 * Auth module responsibility: Sync Firebase users with local database for profile management
 */
object Users : Table("users") {

    val id = varchar("id", 128) // Firebase UID (primary key)
    val email = varchar("email", 255)
    val phone = varchar("phone", 20).nullable()
    val lastActiveAt = timestamp("last_active_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        // Indexes defined in migration, documented here for reference
        index(isUnique = true, email)
        index(isUnique = true, phone)
        index(customIndexName = "idx_users_created_at", columns = arrayOf(createdAt))
    }
}

/**
 * User DTO - Immutable data transfer object
 */
data class User(
    val id: String, // Firebase UID
    val email: String,
    val phone: String?,
    val lastActiveAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Extension function to map ResultRow to User DTO
 */
fun ResultRow.toUser() = User(
    id = this[Users.id],
    email = this[Users.email],
    phone = this[Users.phone],
    lastActiveAt = this[Users.lastActiveAt],
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt]
)
