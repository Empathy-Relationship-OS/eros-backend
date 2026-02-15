package com.eros.users.models

data class UserCityPreference(
    val userId : String,
    val cityId : Long,

    // Timestamps
    val createdAt: java.time.Instant,
)

data class CreateUserCityPreferenceRequest(
    val userId : String,
    val cityId : Long
)

data class DeleteUserCityPreferenceRequest(
    val userId : String,
    val cityId : Long
)

data class DeleteAllUserCityPreferenceRequest(
    val userId : String,
)
