package com.eros.auth.tables

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Users table - Core identity and authentication data
 *
 * This table is owned by the auth module and contains only authentication-related data.
 * Profile data (name, bio, preferences, etc.) belongs in the users module.
 *
 * Auth module responsibility: "Who you are" (identity, credentials, verification)
 */
@OptIn(ExperimentalUuidApi::class)
object Users : Table("users") {

    val id = uuid("id").clientDefault { Uuid.random() }
    val email = varchar("email", 255)
    val phone = varchar("phone", 20).nullable()
    val passwordHash = varchar("password_hash", 255)
    val verificationStatus = varchar("verification_status", 20).default("PENDING")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        // Indexes defined in migration, documented here for reference
        index(isUnique = true, email)
        index(isUnique = true, phone)
        index(customIndexName = "idx_users_verification_status", columns = arrayOf(verificationStatus))
        index(customIndexName = "idx_users_created_at", columns = arrayOf(createdAt))
    }
}

/**
 * User verification status enumeration
 */
enum class VerificationStatus {
    PENDING,
    VERIFIED,
    SUSPENDED;

    companion object {
        fun fromString(value: String): VerificationStatus = valueOf(value.uppercase())
    }
}

/**
 * User DTO - Immutable data transfer object
 */
@OptIn(ExperimentalUuidApi::class)
data class User(
    val id: Uuid,
    val email: String,
    val phone: String?,
    val passwordHash: String,
    val verificationStatus: VerificationStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Extension function to map ResultRow to User DTO
 */
@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toUser() = User(
    id = this[Users.id],
    email = this[Users.email],
    phone = this[Users.phone],
    passwordHash = this[Users.passwordHash],
    verificationStatus = VerificationStatus.fromString(this[Users.verificationStatus]),
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt]
)
