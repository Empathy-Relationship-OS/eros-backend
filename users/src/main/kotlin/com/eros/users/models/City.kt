package com.eros.users.models

import com.eros.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data class for a city.
 *
 * @param cityID - id of the city.
 * @param cityName - Name of the city.
 * @param createdAt - Time of creation.
 * @param updatedAt - Time of late update.
 */
@Serializable
data class City(
    // City id.
    val cityId : Long,

    // City name.
    val cityName : String,

    // Timestamps
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
)

/**
 * Lightweight city DTO without timestamps.
 * Used in API responses and when passing city references between services.
 * Provides only essential information (id and name) for better performance and cleaner APIs.
 *
 * @param cityId - Unique identifier of the city.
 * @param cityName - Name of the city.
 */
@Serializable
data class CityDTO(
    val cityId: Long,
    val cityName: String
)

/**
 * Extension function to convert a full City entity to a lightweight CityDTO.
 * Use this when you need to strip timestamps from the entity for API responses.
 */
fun City.toLightweightDTO() = CityDTO(
    cityId = this.cityId,
    cityName = this.cityName
)

/**
 * Data class for CreateCityRequest - This is used for adding a city to the Cities db table.
 *
 * @param cityName - Name of the city to be added.
 *
 */
@Serializable
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
@Serializable
data class UpdateCityRequest(
    val cityId : Long,
    val newCityName : String
)