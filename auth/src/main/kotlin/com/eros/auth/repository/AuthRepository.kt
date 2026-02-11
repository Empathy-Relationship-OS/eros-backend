package com.eros.auth.repository

import com.eros.auth.tables.User

/**
 * Repository interface for user data management with Firebase Auth integration.
 *
 * This repository handles syncing Firebase-authenticated users with the local database.
 * Firebase handles: passwords, OTP verification, email/phone verification, JWT tokens
 * This repository handles: User profile data storage and retrieval
 *
 * All operations use Exposed ORM with proper transaction management via dbQuery.
 */
interface AuthRepository {

    /**
     * Creates or updates a user from Firebase authentication.
     *
     * This method syncs a Firebase-authenticated user to the local database.
     * If a user with the given Firebase UID exists, it updates their data.
     * If not, it creates a new user record.
     *
     * @param firebaseUid Firebase Authentication user ID (unique identifier)
     * @param email User's email address from Firebase
     * @param phone User's phone number from Firebase (nullable)
     * @return The created or updated User object
     */
    suspend fun createOrUpdateUser(firebaseUid: String, email: String, phone: String?): User

    /**
     * Finds a user by Firebase UID.
     *
     * @param firebaseUid Firebase user ID to search for
     * @return User if found, null otherwise
     */
    suspend fun findByFirebaseUid(firebaseUid: String): User?

    /**
     * Finds a user by email address.
     *
     * @param email Email address to search for
     * @return User if found, null otherwise
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Updates user's last active timestamp to current time.
     *
     * @param firebaseUid Firebase UID of the user to update
     * @return Number of rows updated (1 if successful, 0 if user not found)
     */
    suspend fun updateLastActiveAt(firebaseUid: String): Int

    /**
     * Deletes a user by Firebase UID.
     *
     * Used when a user deletes their Firebase account.
     * Should cascade delete all related user data for GDPR compliance.
     *
     * @param firebaseUid Firebase UID of the user to delete
     * @return Number of rows deleted (1 if successful, 0 if user not found)
     */
    suspend fun deleteUser(firebaseUid: String): Int
}
