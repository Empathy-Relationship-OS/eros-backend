package com.eros.users.table

import com.eros.users.models.UserCityPreference
import com.eros.users.models.UserPreference
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * Database table definition for user city preferences.
 *
 * This junction table establishes a many-to-many relationship between users and cities,
 * allowing users to specify multiple cities where they are willing to date. Each record
 * represents a single user-city preference pairing.
 *
 * The composite primary key ([userId], [cityId]) ensures that a user cannot have duplicate
 * preferences for the same city.
 */
object UserCitiesPreference : Table("user_cities_preference") {

    val userId = varchar("user_id", 128).references(Users.userId)
    val cityId = long("city_id").references(Cities.id)

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(userId, cityId)

}


fun ResultRow.toUserCityPreferenceDTO() = UserCityPreference(
    userId = this[UserCitiesPreference.userId],
    cityId= this[UserCitiesPreference.cityId],
    createdAt = this[UserCitiesPreference.createdAt],
)