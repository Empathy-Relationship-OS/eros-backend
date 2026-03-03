package com.eros.users.table

import com.eros.users.models.City
import com.eros.users.models.CityDTO
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Database table definition for cities.
 *
 * This table stores city information used throughout the application for location-based features
 * such as user preferences and date locations. Cities are referenced by other tables to maintain
 * data consistency and enable efficient querying.
 *
 * The table uses Exposed framework's DSL for type-safe database operations.
 *
 * @see com.eros.users.models.City The domain model representing a city entity
 */
object Cities : Table("cities") {

    // Primary key
    val cityId = long("city_id").autoIncrement()

    // Foreign key to Users table
    val cityName = varchar("city_name", 128).uniqueIndex()

    val longitude = double("longitude")
    val latitude = double("latitude")

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(cityId)

}

fun ResultRow.toCityDTO() = City(
    cityId = this[Cities.cityId],
    cityName = this[Cities.cityName],
    longitude = this[Cities.longitude],
    latitude = this[Cities.latitude],
    createdAt = this[Cities.createdAt],
    updatedAt = this[Cities.updatedAt]
)