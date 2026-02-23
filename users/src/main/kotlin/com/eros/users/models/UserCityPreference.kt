package com.eros.users.models

import com.eros.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data class for UserCityPreference. This is an entry for a user selecting which cities they prefer to have a date in.
 *
 * @param userId - id of the user.
 * @param cityId - id of the city.
 * @param createdAt - Date/Time the preference was created.
 */
@Serializable
data class UserCityPreference(
    val userId : String,
    val cityId : Long,

    // Timestamps
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
)

/**
 * Data class for CreateUserCityPreferenceRequest
 *
 * @param userId - id of User creating the preference.
 * @param cityId - id of the city selected.
 */
@Serializable
data class CreateUserCityPreferenceRequest(
    val userId : String,
    val cityId : Long
)

/**
 * Data class for DeleteUserCityPreferenceRequest.
 *
 * @param userId - id of User deleting/removing the preference.
 * @param cityId - id of the City that will be removed from the users preferences.
 */
@Serializable
data class DeleteUserCityPreferenceRequest(
    val userId : String,
    val cityId : Long
)

/**
 * Data class for DeleteAllUserCityPreferenceRequest.
 *
 * @param userId - The id of the user to have all their city preferences removed.
 */
@Serializable
data class DeleteAllUserCityPreferenceRequest(
    val userId : String,
)
