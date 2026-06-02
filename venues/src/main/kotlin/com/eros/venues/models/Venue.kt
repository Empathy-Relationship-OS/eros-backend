package com.eros.venues.models

import com.eros.common.serializers.LocalDateSerializer
import kotlinx.serialization.Serializable
import com.eros.venues.DressCode
import com.eros.venues.IndoorOutdoor
import com.eros.venues.PriceRange
import java.time.Instant
import java.time.LocalDate

/**
 * Domain object for a venue that hosts a date.
 */
data class Venue(
    val venueId: Long,
    val name: String,
    val description: String,
    val address: String,
    val cityId: Long,
    val latitude: Double,
    val longitude: Double,
    val priceRange: PriceRange,
    val maxCapacity: Int,
    val reservationRequired: Boolean,
    val partnerInstructions: String,
    val disabledFriendly: Boolean,
    val indoorOutdoor: IndoorOutdoor,
    val parkingAvailable: Boolean,
    val websiteUrl: String?,
    val dressCode: DressCode,
    val activeFrom: LocalDate,
    val activeTo: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)


/**
 * Request DTO for a business/employee to create a venue for dates.
 */
@Serializable
data class CreateVenueRequest(
    val name: String,
    val description: String,
    val address: String,
    val cityId: Long,
    val latitude: Double,
    val longitude: Double,
    val priceRange: PriceRange,
    val maxCapacity: Int,
    val reservationRequired: Boolean,
    val partnerInstructions: String,
    val disabledFriendly: Boolean,
    val indoorOutdoor: IndoorOutdoor,
    val parkingAvailable: Boolean,
    val websiteUrl: String?,
    val dressCode: DressCode,
    @Serializable(with = LocalDateSerializer::class)
    val activeFrom: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val activeTo: LocalDate?
)


/**
 * Request DTO for fields a venue can self update
 */
@Serializable
data class UpdateVenueRequest(
    val name: String?,
    val description: String?,
    val address: String?,
    val maxCapacity: Int?,
    val reservationRequired: Boolean?,
    val partnerInstructions: String?,
    val disabledFriendly: Boolean?,
    val indoorOutdoor: IndoorOutdoor?,
    val parkingAvailable: Boolean?,
    val websiteUrl: String?,
    val dressCode: DressCode?,
)


/**
 * Request DTO for admin-only fields to update a venue.
 */
@Serializable
data class AdminUpdateVenueRequest(
    val name: String?,
    val description: String?,
    val address: String?,
    val cityId: Long?,
    val latitude: Double?,
    val longitude: Double?,
    val priceRange: PriceRange?,
    val maxCapacity: Int?,
    val reservationRequired: Boolean?,
    val partnerInstructions: String?,
    val disabledFriendly: Boolean?,
    val indoorOutdoor: IndoorOutdoor?,
    val parkingAvailable: Boolean?,
    val websiteUrl: String?,
    val dressCode: DressCode?,
    @Serializable(with = LocalDateSerializer::class)
    val activeFrom: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val activeTo: LocalDate?
)


/**
 * DTO for venue.
 */
@Serializable
data class VenueDTO(
    val name: String,
    val description: String,
    val address: String,
    val cityId: Long,
    val latitude: Double,
    val longitude: Double,
    val priceRange: PriceRange,
    val maxCapacity: Int,
    val reservationRequired: Boolean,
    val partnerInstructions: String,
    val disabledFriendly: Boolean,
    val indoorOutdoor: IndoorOutdoor,
    val parkingAvailable: Boolean,
    val websiteUrl: String?,
    val dressCode: DressCode,
    @Serializable(with = LocalDateSerializer::class)
    val activeFrom: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val activeTo: LocalDate?
)


/**
 * Function for converting a venue domain object to a DTO.
 */
fun Venue.toDTO() = VenueDTO(
    name = this.name,
    description = this.description,
    address = this.address,
    cityId = this.cityId,
    latitude = this.latitude,
    longitude = this.longitude,
    priceRange = this.priceRange,
    maxCapacity = this.maxCapacity,
    reservationRequired = this.reservationRequired,
    partnerInstructions = this.partnerInstructions,
    disabledFriendly = this.disabledFriendly,
    indoorOutdoor = this.indoorOutdoor,
    parkingAvailable = this.parkingAvailable,
    websiteUrl = this.websiteUrl,
    dressCode = this.dressCode,
    activeFrom = this.activeFrom,
    activeTo = this.activeTo
)