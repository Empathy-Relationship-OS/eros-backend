package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.User

/**
 * Repository interface for user data management.
 *
 * Extends [IBaseDAO] with [String] (Firebase UID) as the primary key type.
 *
 * Firebase handles: passwords, OTP verification, email/phone verification, JWT tokens.
 * This repository handles: User profile CRUD operations.
 *
 * All operations use Exposed ORM with proper transaction management via dbQuery.
 */
interface UserRepository : IBaseDAO<String, User> {

    /**
     * Finds a user by email address.
     *
     * @param email Email address to search for.
     * @return User if found, null otherwise.
     */
    suspend fun findByEmail(email: String): User?
}
