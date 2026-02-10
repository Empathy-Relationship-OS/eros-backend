package com.eros.auth.models

import kotlinx.serialization.Serializable

/**
 * Response model for authentication.
 *
 * @property accessToken JWT token for authenticated requests
 * @property tokenType Type of token
 * @property issuedAt Epoch Time token was issued
 * @property expires Epoch Time token expires
 * @property user Summary information about the authenticated user
 */
@Serializable
data class AuthResponse (
    val accessToken: String,
    val tokenType: String,
    val issuedAt: Long,
    val expires: Long,
    val user: UserSummary
)
