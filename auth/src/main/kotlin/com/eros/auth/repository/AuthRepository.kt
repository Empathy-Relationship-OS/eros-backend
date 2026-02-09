package com.eros.auth.repository

import com.eros.auth.tables.OtpVerificationResult
import com.eros.auth.tables.User
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Repository interface for authentication-related database operations.
 *
 * Provides data access methods for user management and OTP verification.
 * All operations use Exposed ORM with proper transaction management via dbQuery.
 */
@OptIn(ExperimentalUuidApi::class)
interface AuthRepository {

    /**
     * Creates a new user with hashed password.
     *
     * @param email User's email address (must be unique)
     * @param phone User's phone number (optional, must be unique if provided)
     * @param plainPassword Plain text password to be hashed
     * @return The created User object
     * @throws org.jetbrains.exposed.v1.exceptions.ExposedSQLException if email or phone already exists
     */
    suspend fun createUser(email: String, phone: String?, plainPassword: String): User

    /**
     * Finds a user by email address.
     *
     * @param email Email address to search for
     * @return User if found, null otherwise
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Checks if an email address is already registered.
     *
     * @param email Email address to check
     * @return true if email exists, false otherwise
     */
    suspend fun existsByEmail(email: String): Boolean

    /**
     * Checks if a phone number is already registered.
     *
     * @param phone Phone number to check
     * @return true if phone exists, false otherwise
     */
    suspend fun existsByPhone(phone: String): Boolean

    /**
     * Updates user's verification status to VERIFIED.
     *
     * @param userId ID of the user to update
     * @return Number of rows updated (1 if successful, 0 if user not found)
     */
    suspend fun updateVerificationStatus(userId: Uuid): Int

    /**
     * Updates user's last active timestamp to current time.
     *
     * @param userId ID of the user to update
     * @return Number of rows updated (1 if successful, 0 if user not found)
     */
    suspend fun updateLastActiveAt(userId: Uuid): Int

    /**
     * Stores a new OTP for phone verification.
     *
     * Automatically hashes the OTP before storage and sets expiry time.
     * Deletes any existing OTP for the same phone number before inserting.
     *
     * @param phoneNumber Phone number to associate with OTP
     * @param plainOtp Plain text OTP to be hashed and stored
     * @param expiryMinutes Number of minutes until OTP expires (default: 10)
     * @return The ID of the created OTP record
     */
    suspend fun storeOtp(
        phoneNumber: String,
        plainOtp: String,
        expiryMinutes: Long = 10L
    ): Uuid

    /**
     * Verifies an OTP against the stored hash.
     *
     * Checks if OTP is valid, not expired, and attempts haven't been exceeded.
     * Increments attempt counter on failure.
     * Deletes OTP record on successful verification.
     *
     * @param phoneNumber Phone number to verify
     * @param plainOtp Plain text OTP to verify
     * @return OtpVerificationResult indicating the outcome
     */
    suspend fun verifyOtp(phoneNumber: String, plainOtp: String): OtpVerificationResult
}
