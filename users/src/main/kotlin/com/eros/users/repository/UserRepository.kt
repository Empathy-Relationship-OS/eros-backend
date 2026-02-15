package com.eros.users.repository

import com.eros.users.models.CreateUserRequest
import com.eros.users.models.UpdateUserRequest
import com.eros.users.models.User

/**
 * Repository interface for user data management.
 *
 * This repository handles user profile data storage and retrieval.
 * Firebase handles: passwords, OTP verification, email/phone verification, JWT tokens
 * This repository handles: User profile CRUD operations
 *
 * All operations use Exposed ORM with proper transaction management via dbQuery.
 */
interface UserRepository {

    /**
     * Creates a new user profile.
     *
     * @param request CreateUserRequest containing all required user profile data
     * @return The created User
     */
    suspend fun createUser(request: CreateUserRequest): User

    /**
     * Updates an existing user profile.
     *
     * @param userId Firebase UID of the user to update
     * @param request UpdateUserRequest containing fields to update
     * @return The updated User, or null if user not found
     */
    suspend fun updateUser(userId: String, request: UpdateUserRequest): User?

    /**
     * Finds a user by Firebase UID.
     *
     * @param userId Firebase user ID to search for
     * @return User if found, null otherwise
     */
    suspend fun findByUserId(userId: String): User?

    /**
     * Finds a user by email address.
     *
     * @param email Email address to search for
     * @return User if found, null otherwise
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Deletes a user by Firebase UID (soft delete).
     *
     * Used when a user deletes their account for GDPR compliance.
     *
     * @param userId Firebase UID of the user to delete
     * @return Number of rows updated (1 if successful, 0 if user not found)
     */
    suspend fun deleteUser(userId: String): Int

    /**
     * Checks if a user exists by Firebase UID.
     *
     * @param userId Firebase UID to check
     * @return True if user exists, false otherwise
     */
    suspend fun userExists(userId: String): Boolean
}
