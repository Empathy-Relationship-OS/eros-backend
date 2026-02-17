package com.eros.users.models

import java.time.Instant

/**
 * Data class for a city.
 *
 * @param cityID - id of the city.
 * @param cityName - Name of the city.
 * @param createdAt - Time of creation.
 * @param updatedAt - Time of late update.
 */
data class City(
    // City id.
    val cityId : Long,

    // City name.
    val cityName : String,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Data class for CreateCityRequest - This is used for adding a city to the Cities db table.
 *
 * @param cityName - Name of the city to be added.
 *
 */
data class CreateCityRequest(
    val cityName : String
)

/**
 * Data class for UpdateCityRequest - This is used for updating the name of a city in the Cities db table.
 *
 * @param cityId - The id of the city to be updated.
 * @param newCityName - New name of the city of [cityId]
 *
 */
data class UpdateCityRequest(
    val cityId : Long,
    val newCityName : String
)