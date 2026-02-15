package com.eros.users.service

import com.eros.users.models.CreateUserRequest
import com.eros.users.models.UpdateUserRequest
import com.eros.users.models.User
import com.eros.users.repository.UserRepository

/**
 * Service layer for user operations.
 *
 * This service acts as a business logic layer between the routes and repository,
 * handling validation, coordination, and any cross-cutting concerns.
 */
class UserService(private val userRepository: UserRepository) {

    /**
     * Creates a new user profile.
     *
     * @param request CreateUserRequest containing all required user profile data
     * @return The created User
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun createUser(request: CreateUserRequest): User {
        return userRepository.createUser(request)
    }

    /**
     * Updates an existing user profile.
     *
     * @param userId Firebase UID of the user to update
     * @param request UpdateUserRequest containing fields to update
     * @return The updated User, or null if user not found
     * @throws IllegalArgumentException if input validation fails
     */
    suspend fun updateUser(userId: String, request: UpdateUserRequest): User? {
        return userRepository.updateUser(userId, request)
    }

    /**
     * Finds a user by Firebase UID.
     *
     * @param userId Firebase user ID to search for
     * @return User if found, null otherwise
     */
    suspend fun findByUserId(userId: String): User? {
        return userRepository.findByUserId(userId)
    }

    /**
     * Finds a user by email address.
     *
     * @param email Email address to search for
     * @return User if found, null otherwise
     */
    suspend fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    /**
     * Deletes a user by Firebase UID (soft delete).
     *
     * Used when a user deletes their account for GDPR compliance.
     *
     * @param userId Firebase UID of the user to delete
     * @return Number of rows updated (1 if successful, 0 if user not found)
     */
    suspend fun deleteUser(userId: String): Int {
        return userRepository.deleteUser(userId)
    }

    /**
     * Checks if a user exists by Firebase UID.
     *
     * @param userId Firebase UID to check
     * @return True if user exists, false otherwise
     */
    suspend fun userExists(userId: String): Boolean {
        return userRepository.userExists(userId)
    }
}
