package com.eros.venues.service

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.venues.DressCode
import com.eros.venues.IndoorOutdoor
import com.eros.venues.PriceRange
import com.eros.venues.models.AdminUpdateVenueRequest
import com.eros.venues.models.CreateVenueRequest
import com.eros.venues.models.UpdateVenueRequest
import com.eros.venues.models.Venue
import com.eros.venues.repository.VenueRepository
import com.eros.venues.table.Venues
import java.time.Clock

class VenueService(
    private val venueRepository: VenueRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * This function is used to create a venue in the database that hosts a date location.
     *
     * @param [CreateVenueRequest] data class containing all the necessary data for a business or employee to create a
     *        venue in the database.
     * @return [Venue] of the created venue.
     */
    suspend fun createVenue(request: CreateVenueRequest) : Venue = dbQuery {
        val now = clock.instant()
        val venue = Venue(
            venueId = 0L, // Placeholder value for the auto-increment PK
            name = request.name,
            description = request.description,
            address = request.address,
            cityId = request.cityId,
            longitude = request.longitude,
            latitude = request.latitude,
            priceRange = request.priceRange,
            maxCapacity = request.maxCapacity,
            reservationRequired = request.reservationRequired,
            partnerInstructions = request.partnerInstructions,
            disabledFriendly = request.disabledFriendly,
            indoorOutdoor = request.indoorOutdoor,
            parkingAvailable = request.parkingAvailable,
            websiteUrl = request.websiteUrl,
            dressCode = request.dressCode,
            activeFrom = request.activeFrom,
            activeTo = request.activeTo,
            createdAt = now,
            updatedAt = now,
        )
        venueRepository.create(venue)
    }


    /**
     * Function for a venue to alter the details of a venue.
     */
    suspend fun updateVenue(venueId: Long, request: UpdateVenueRequest) : Venue? = dbQuery {
        val now = clock.instant()
        val existing = venueRepository.findById(venueId) ?: throw NotFoundException("Venue not found")
        val updatedVenue = Venue(
            venueId = venueId,
            name = request.name ?: existing.name,
            description = request.description ?: existing.description,
            address = request.address ?: existing.address,
            cityId = existing.cityId,
            longitude = existing.longitude,
            latitude = existing.latitude,
            priceRange = existing.priceRange,
            maxCapacity = request.maxCapacity ?: existing.maxCapacity,
            reservationRequired = request.reservationRequired ?: existing.reservationRequired,
            partnerInstructions = request.partnerInstructions ?: existing.partnerInstructions,
            disabledFriendly = request.disabledFriendly ?: existing.disabledFriendly,
            indoorOutdoor = request.indoorOutdoor ?: existing.indoorOutdoor,
            parkingAvailable = request.parkingAvailable ?: existing.parkingAvailable,
            websiteUrl = request.websiteUrl ?: existing.websiteUrl,
            dressCode = request.dressCode ?: existing.dressCode,
            activeFrom = existing.activeFrom,
            activeTo = existing.activeTo,
            createdAt = existing.createdAt,
            updatedAt = now,
        )
        venueRepository.update(venueId, updatedVenue)
    }

    /**
     * Function for a venue to alter the details of a venue.
     */
    suspend fun updateVenueAdmin(venueId: Long, request: AdminUpdateVenueRequest) : Venue? = dbQuery {
        val now = clock.instant()
        val existing = venueRepository.findById(venueId) ?: throw NotFoundException("Venue not found")
        val updatedVenue = Venue(
            venueId = venueId,
            name =  existing.name,
            description = existing.description,
            address = existing.address,
            cityId = request.cityId ?: existing.cityId,
            longitude = request.longitude ?: existing.longitude,
            latitude = request.latitude ?: existing.latitude,
            priceRange = existing.priceRange,
            maxCapacity = existing.maxCapacity,
            reservationRequired = existing.reservationRequired,
            partnerInstructions = existing.partnerInstructions,
            disabledFriendly = existing.disabledFriendly,
            indoorOutdoor = existing.indoorOutdoor,
            parkingAvailable = existing.parkingAvailable,
            websiteUrl = existing.websiteUrl,
            dressCode = existing.dressCode,
            activeFrom = request.activeFrom ?: existing.activeFrom,
            activeTo = request.activeTo ?: existing.activeTo,
            createdAt = existing.createdAt,
            updatedAt = now,
        )
        venueRepository.update(venueId, updatedVenue)
    }

}