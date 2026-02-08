package com.eros.auth.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents the payload/claims stored in a JWT token.
 *
 * @property sub The subject (user ID) of the token
 * @property email The email address of the user
 * @property roles The list of roles assigned to the user (e.g., "user", "admin")
 */
@Serializable
data class JwtPayload(
    val sub: String,  // Subject (userId)
    val email: String,
    val roles: List<String>
) {
    companion object {
        /**
         * Creates a JwtPayload from a UUID subject.
         * Convenience method for creating tokens with UUID-based user IDs.
         */
        fun fromUserId(userId: UUID, email: String, roles: List<String> = listOf("user")): JwtPayload {
            return JwtPayload(
                sub = userId.toString(),
                email = email,
                roles = roles
            )
        }
    }
}
