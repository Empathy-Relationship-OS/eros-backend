package com.eros.users.models

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Domain object for a city.
 *
 * @param cityId id of the city.
 * @param cityName Name of the city.
 * @param longitude Longitude of the city centre.
 * @param latitude Latitude of the city centre.
 * @param createdAt Time of creation.
 * @param updatedAt Time of late update.
 */
data class City(
    // City id.
    val cityId: Long,

    // City name.
    val cityName: String,

    // City location.
    val longitude: Double,
    val latitude: Double,

    // Timestamps
    val createdAt: Instant,
    val updatedAt: Instant,
){
    init {
        require(cityName.isNotBlank()) {"City name must not be empty." }
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}


/**
 * City DTO without timestamps.
 * Used in API responses and when passing city references between services.
 * Provides only essential information (id, name, and longitude and latitude of the city centre) for better performance and cleaner APIs.
 *
 * @param cityId - Unique identifier of the city.
 * @param cityName - Name of the city.
 * @param longitude Longitude of the city centre.
 * @param latitude Latitude of the city centre.
 */
@Serializable
data class CityDTO(
    val cityId: Long,
    val cityName: String,
    val longitude: Double,
    val latitude: Double
){
    init {
        require(cityName.isNotBlank()) {"City name must not be empty." }
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}


/**
 * Extension function to convert a full City entity to a lightweight CityDTO.
 * Use this when you need to strip timestamps from the entity for API responses.
 */
fun City.toDTO() = CityDTO(
    cityId = this.cityId,
    cityName = this.cityName,
    longitude = this.longitude,
    latitude = this.latitude
)


/**
 * Data class for CreateCityRequest - This is used for adding a city to the Cities db table.
 *
 * @param cityName - Name of the city to be added.
 * @param longitude Longitude of the city centre.
 * @param latitude Latitude of the city centre.
 *
 */
@Serializable
data class CreateCityRequest(
    val cityName: String,
    val longitude: Double,
    val latitude: Double
){
    init {
        require(cityName.isNotBlank()) {"City name must not be empty." }
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}


/**
 * Data class for UpdateCityRequest - This is used for updating the name of a city in the Cities db table.
 *
 * @param cityId The id of the city to be updated.
 * @param newCityName New name of the city of [cityId]
 * @param newCityLongitude Longitude of the city centre.
 * @param newCityLatitude Latitude of the city centre.
 *
 */
@Serializable
data class UpdateCityRequest(
    val cityId: Long,
    val newCityName: String? = null,
    val newCityLongitude: Double? = null,
    val newCityLatitude: Double? = null,
){
    init {
        if (newCityName != null){
            require(newCityName.isNotBlank()) {"City name must not be empty." }
        }
        if (newCityLatitude != null) {
            require(newCityLatitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        }
        if (newCityLongitude != null){
            require(newCityLongitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        }
    }
}