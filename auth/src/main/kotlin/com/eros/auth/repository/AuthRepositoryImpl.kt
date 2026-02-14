package com.eros.auth.repository

import com.eros.auth.tables.*
import com.eros.database.dbQuery
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.time.Clock
import java.time.Instant

/**
 * Implementation of AuthRepository using Exposed ORM with Firebase integration.
 *
 * This repository syncs Firebase-authenticated users with the local database.
 * All database operations are wrapped in dbQuery for proper transaction management
 * and IO dispatcher execution.
 *
 * @param clock Clock instance for time-based operations (defaults to system UTC)
 */
class AuthRepositoryImpl(
    private val clock: Clock = Clock.systemUTC()
) : AuthRepository {

    override suspend fun createOrUpdateUser(firebaseUid: String, email: String, phone: String?): UpsertResult = dbQuery {
        val now = Instant.now(clock)

        // Check if user exists before upsert
        val existingUser = Users.selectAll()
            .where { Users.id eq firebaseUid }
            .singleOrNull()

        val wasCreated = existingUser == null

        // Use upsert to create or update user based on Firebase UID
        Users.upsert(Users.id) { row ->
            row[Users.id] = firebaseUid
            row[Users.email] = email
            row[Users.phone] = phone
            row[Users.updatedAt] = now
            // createdAt only set on insert (not updated on conflict)
        }

        // Fetch and return the user
        val user = Users.selectAll()
            .where { Users.id eq firebaseUid }
            .single()
            .toUser()

        UpsertResult(user = user, wasCreated = wasCreated)
    }

    override suspend fun findByFirebaseUid(firebaseUid: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.id eq firebaseUid }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll()
            .where { Users.email.lowerCase() eq email.lowercase() }
            .singleOrNull()
            ?.toUser()
    }

    override suspend fun updateLastActiveAt(firebaseUid: String): Int = dbQuery {
        val now = Instant.now(clock)
        Users.update({ Users.id eq firebaseUid }) {
            it[lastActiveAt] = now
            it[updatedAt] = now
        }
    }

    override suspend fun deleteUser(firebaseUid: String): Int = dbQuery {
        Users.deleteWhere { Users.id eq firebaseUid }
    }
}
