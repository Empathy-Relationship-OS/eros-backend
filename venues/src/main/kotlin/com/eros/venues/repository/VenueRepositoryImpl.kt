package com.eros.venues.repository

import com.eros.database.repository.BaseDAOImpl
import com.eros.venues.models.Venue
import com.eros.venues.table.Venues
import com.eros.venues.table.toVenue
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import java.time.Clock


class VenueRepositoryImpl (
    private val clock: Clock = Clock.systemUTC()
) : BaseDAOImpl<Long, Venue>(Venues, Venues.venueId), VenueRepository {

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------
    override fun ResultRow.toDomain() : Venue = toVenue()

    override fun toStatement(statement: UpdateBuilder<*>, entity: Venue) {
        statement.apply {
            this[Venues.name] = entity.name
            this[Venues.description] = entity.description
            this[Venues.address] = entity.address
            this[Venues.cityId] = entity.cityId
            this[Venues.latitude] = entity.latitude
            this[Venues.longitude] = entity.longitude
            this[Venues.priceRange] = entity.priceRange.name
            this[Venues.maxCapacity] = entity.maxCapacity
            this[Venues.reservationRequired] = entity.reservationRequired
            this[Venues.partnerInstructions] = entity.partnerInstructions
            this[Venues.disabledFriendly] = entity.disabledFriendly
            this[Venues.indoorOutdoor] = entity.indoorOutdoor.name
            this[Venues.parkingAvailable] = entity.parkingAvailable
            this[Venues.websiteUrl] = entity.websiteUrl
            this[Venues.dressCode] = entity.dressCode.name
            this[Venues.activeFrom] = entity.activeFrom
            this[Venues.activeTo] = entity.activeTo
            this[Venues.createdAt] = entity.createdAt
            this[Venues.updatedAt] = entity.updatedAt
        }
    }

    // -------------------------------------------------------------------------
    // IBaseDAO overrides
    // -------------------------------------------------------------------------



}