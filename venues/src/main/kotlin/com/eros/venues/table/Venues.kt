package com.eros.venues.table

import com.eros.users.table.Cities
import com.eros.venues.DressCode
import com.eros.venues.IndoorOutdoor
import com.eros.venues.PriceRange
import com.eros.venues.models.Venue
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Venues : Table("venues") {

    // Primary key
    val venueId = long("venue_id").autoIncrement()

    // Basic information
    val name = varchar("name", 128)
    val description = varchar("description", 128)

    // Location
    val address = varchar("address", 128)
    val cityId = long("city_id").references(Cities.cityId, onDelete = ReferenceOption.CASCADE)
    val latitude = double("latitude")
    val longitude = double("longitude")

    // Business information
    val priceRange = varchar("price_range", 16)
    val maxCapacity = integer("max_capacity")
    val reservationRequired = bool("reservation_required")
    val partnerInstructions = varchar("partner_instructions", 256)
    val disabledFriendly = bool("disabled_friendly")
    val indoorOutdoor = varchar("indoor_outdoor", 16)
    val parkingAvailable = bool("parking_available")

    val websiteUrl = varchar("website_url", 256).nullable()
    val dressCode = varchar("dress_code", 16)

    val activeFrom = date("active_from")
    val activeTo = date("active_to").nullable()

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(venueId)

}

fun ResultRow.toVenue() = Venue (
    venueId = this[Venues.venueId],
    name = this[Venues.name],
    description = this[Venues.description],
    address = this[Venues.address],
    cityId = this[Venues.cityId],
    longitude = this[Venues.longitude],
    latitude = this[Venues.latitude],
    priceRange = PriceRange.valueOf(this[Venues.priceRange]),
    maxCapacity = this[Venues.maxCapacity],
    reservationRequired = this[Venues.reservationRequired],
    partnerInstructions = this[Venues.partnerInstructions],
    disabledFriendly = this[Venues.disabledFriendly],
    indoorOutdoor = IndoorOutdoor.valueOf(this[Venues.indoorOutdoor]),
    parkingAvailable = this[Venues.parkingAvailable],
    websiteUrl = this[Venues.websiteUrl],
    dressCode = DressCode.valueOf(this[Venues.dressCode]),
    activeFrom = this[Venues.activeFrom],
    activeTo = this[Venues.activeTo],
    createdAt = this[Venues.createdAt],
    updatedAt = this[Venues.updatedAt],
)