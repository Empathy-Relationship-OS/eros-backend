package com.eros.users.models

import java.time.Instant

data class City(
    // City id.
    val id : Long,

    // City name.
    val cityName : String,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateCityRequest(
    val id: Long,
    val cityName : String
)

data class UpdateCityRequest(
    val cityName : String,
)