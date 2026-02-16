package com.eros.users.models

import java.time.Instant

data class City(
    // City id.
    val cityId : Long,

    // City name.
    val cityName : String,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateCityRequest(
    val cityName : String
)

data class UpdateCityRequest(
    val cityId : Long,
    val newCityName : String
)