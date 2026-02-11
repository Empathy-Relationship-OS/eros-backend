package com.eros.auth.models

import kotlinx.serialization.Serializable

/**
 * Response model for syncing a Firebase-authenticated user with backend.
 *
 * After Firebase authentication on the client side, this response is returned
 * when the user's profile is synced/created in the backend database.
 *
 * Note: The Firebase ID token is managed client-side. This response only
 * contains the synced user profile data from the backend.
 *
 * @property user Summary information about the synced user
 * @property isNewUser Whether this was a new user creation (true) or update (false)
 */
@Serializable
data class SyncProfileResponse(
    val user: UserSummary,
    val isNewUser: Boolean
)
