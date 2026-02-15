package com.eros.users.table

import com.eros.users.models.UserCityPreference
import com.eros.users.models.UserPreference
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object UserCitiesPreference : Table("user_preference_cities") {

    val userId = varchar("user_id", 128).references(Users.userId)
    val cityId = long("id").references(Cities.id)

    // Timestamps
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(userId,cityId)

    init {
        // Ensure a user does not have the same city name stored multiple times.
        uniqueIndex("unique_city_name_per_user", userId, cityId)
    }
}


fun ResultRow.toUserCityPreferenceDTO() = UserCityPreference(
    userId = this[UserCitiesPreference.userId],
    cityId= this[UserCitiesPreference.cityId],
    createdAt = this[UserCitiesPreference.createdAt],
)