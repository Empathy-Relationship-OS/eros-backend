package com.eros.venues.models

import com.eros.venues.DressCode
import com.eros.venues.IndoorOutdoor
import com.eros.venues.PriceRange
import java.time.Instant
import java.time.LocalDate

/**
 * The following is to provide a complete test venue object.
 */
fun createTestVenue(
    venueId: Long = 1L,
    name: String = "Test Venue",
    description: String = "A test venue description",
    address: String = "123 Test Street",
    cityId: Long = 1L,
    latitude: Double = 51.5074,
    longitude: Double = -0.1278,
    priceRange: PriceRange = PriceRange.MID_RANGE,
    maxCapacity: Int = 100,
    reservationRequired: Boolean = false,
    partnerInstructions: String = "Test partner instructions",
    disabledFriendly: Boolean = true,
    indoorOutdoor: IndoorOutdoor = IndoorOutdoor.INDOOR,
    parkingAvailable: Boolean = true,
    websiteUrl: String? = "https://testvenue.com",
    dressCode: DressCode = DressCode.SMART_CASUAL,
    activeFrom: LocalDate = LocalDate.now(),
    activeTo: LocalDate? = null,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
): Venue = Venue(
    venueId = venueId,
    name = name,
    description = description,
    address = address,
    cityId = cityId,
    latitude = latitude,
    longitude = longitude,
    priceRange = priceRange,
    maxCapacity = maxCapacity,
    reservationRequired = reservationRequired,
    partnerInstructions = partnerInstructions,
    disabledFriendly = disabledFriendly,
    indoorOutdoor = indoorOutdoor,
    parkingAvailable = parkingAvailable,
    websiteUrl = websiteUrl,
    dressCode = dressCode,
    activeFrom = activeFrom,
    activeTo = activeTo,
    createdAt = createdAt,
    updatedAt = updatedAt,
)


/**
 * Function for creating a completed [CreateVenueRequest] object in use of testing.
 */
fun createTestCreateVenueRequest(
    name: String = "Test Venue",
    description: String = "A test venue description",
    address: String = "123 Test Street",
    cityId: Long = 1L,
    latitude: Double = 51.5074,
    longitude: Double = -0.1278,
    priceRange: PriceRange = PriceRange.MID_RANGE,
    maxCapacity: Int = 100,
    reservationRequired: Boolean = false,
    partnerInstructions: String = "Test partner instructions",
    disabledFriendly: Boolean = true,
    indoorOutdoor: IndoorOutdoor = IndoorOutdoor.INDOOR,
    parkingAvailable: Boolean = true,
    websiteUrl: String? = "https://testvenue.com",
    dressCode: DressCode = DressCode.SMART_CASUAL,
    activeFrom: LocalDate = LocalDate.now(),
    activeTo: LocalDate? = null,
): CreateVenueRequest = CreateVenueRequest(
    name = name,
    description = description,
    address = address,
    cityId = cityId,
    latitude = latitude,
    longitude = longitude,
    priceRange = priceRange,
    maxCapacity = maxCapacity,
    reservationRequired = reservationRequired,
    partnerInstructions = partnerInstructions,
    disabledFriendly = disabledFriendly,
    indoorOutdoor = indoorOutdoor,
    parkingAvailable = parkingAvailable,
    websiteUrl = websiteUrl,
    dressCode = dressCode,
    activeFrom = activeFrom,
    activeTo = activeTo,
)


//todo: Complete basic tests and validations
class VenueTest {
}