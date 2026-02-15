package com.eros.users.table

import com.eros.users.models.City
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Cities : Table("cities") {

    // Primary key
    val id = long("id").autoIncrement()

    // Foreign key to Users table
    val cityName = varchar("city_name", 128).uniqueIndex()

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

}

fun ResultRow.toCityDTO() = City(
    id = this[Cities.id],
    cityName = this[Cities.cityName],
    createdAt = this[Cities.createdAt],
    updatedAt = this[Cities.updatedAt]
)
