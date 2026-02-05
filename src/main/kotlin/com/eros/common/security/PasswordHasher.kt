package com.eros.common.security

import org.mindrot.jbcrypt.BCrypt

/**
 * Provides utility functions for hashing and verifying passwords using the BCrypt algorithm.
 */
object PasswordHasher {

    /**
     * The log rounds used for the BCrypt algorithm.
     */
    private const val COST_FACTOR: Int = 12

    /**
     * Hashes a valid plain-text password using the BCrypt algorithm.
     *
     * @param password The plain-text password to be hashed.
     * @return Hashed string using BCrypt.
     */
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST_FACTOR))
    }

    /**
     * Verifies a plain-text password against an existing BCrypt hash.
     *
     * @param password The plain-text password to check.
     * @param hashedPassword The already computed hash to verify against.
     * @return `true` if the password matches the hash otherwise `false`.
     */
    fun verify(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }
}