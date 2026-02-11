package com.eros.auth.models

import kotlinx.serialization.Serializable

/**
 * Summary model for user information synced from Firebase Auth.
 *
 * @property userId Firebase UID (unique identifier)
 * @property email User's email address (from Firebase)
 * @property phone User's phone number (from Firebase, nullable)
 * @property emailVerified Whether the user's email is verified in Firebase
 */
@Serializable
data class UserSummary(
    val userId: String, // Firebase UID
    val email: String,
    val phone: String?,
    val emailVerified: Boolean
)