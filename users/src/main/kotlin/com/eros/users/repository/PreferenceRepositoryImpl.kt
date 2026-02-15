package com.eros.users.repository

import com.eros.database.dbQuery
import com.eros.users.models.*
import com.eros.users.table.Cities
import com.eros.users.table.UserCitiesPreference
import com.eros.users.table.UserPreferences
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant

class PreferenceRepositoryImpl(private val clock: Clock = Clock.systemUTC()) : PreferenceRepository {

    override suspend fun createPreferences(request: CreatePreferenceRequest): UserPreference {
        val now = Instant.now(clock)

        UserPreferences.insert { row ->
            row[userId] = request.userId
            row[genderIdentities] = request.genderIdentities.map { it.name }
            row[ageRangeMin] = request.ageRangeMin
            row[ageRangeMax] = request.ageRangeMax
            row[heightRangeMin] = request.heightRangeMin
            row[heightRangeMax] = request.heightRangeMax
            row[ethnicities] = request.ethnicity.map { it.name }
            row[dateLanguages] = request.dateLanguages.map { it.name }
            row[dateActivities] = request.dateActivities.map { it.name }
            row[dateLimit] = request.dateLimit
            row[createdAt] = now
            row[updatedAt] = now
        }

        // Insert city preferences into junction table.
        request.dateCities.forEach { cityId ->
            UserCitiesRepositoryImpl().addUserCityPreference(CreateUserCityPreferenceRequest(request.userId, cityId))
        }

        return getUserPreferenceWithCities(request.userId)
    }

    override suspend fun updatePreference(preferenceId: Long, request: UpdatePreferenceRequest): UserPreference {
        val now = Instant.now(clock)

        UserPreferences.update({ UserPreferences.id eq preferenceId }) { row ->
            row[genderIdentities] = request.genderIdentities.map { it.name }
            row[ageRangeMin] = request.ageRangeMin
            row[ageRangeMax] = request.ageRangeMax
            row[heightRangeMin] = request.heightRangeMin
            row[heightRangeMax] = request.heightRangeMax
            row[ethnicities] = request.ethnicity.map { it.name }
            row[dateLanguages] = request.dateLanguages.map { it.name }
            row[dateActivities] = request.dateActivities.map { it.name }
            row[dateLimit] = request.dateLimit
            row[updatedAt] = now
        }

        // Delete existing city preferences
        UserCitiesRepositoryImpl().deleteAllUserCityPreference(DeleteAllUserCityPreferenceRequest(request.userId))

        // Insert city preferences into junction table.
        request.dateCities.forEach { cityId ->
            UserCitiesRepositoryImpl().addUserCityPreference(CreateUserCityPreferenceRequest(request.userId, cityId))
        }

        return getUserPreferenceWithCities(request.userId)
    }


    override suspend fun getUserPreferenceWithCities(userId: String): UserPreference {
        // Get base preferences
        val prefs = UserPreferences.selectAll()
            .where { UserPreferences.userId eq userId }
            .single()

        // Get cities via join
        val cities = (Cities innerJoin UserCitiesPreference)
            .selectAll()
            .where { UserCitiesPreference.userId eq userId }
            .map { row ->
                City(
                    id = row[Cities.id],
                    cityName = row[Cities.cityName],
                    createdAt = row[Cities.createdAt],
                    updatedAt = row[Cities.updatedAt]
                )
            }

        return UserPreference(
            id = prefs[UserPreferences.id],
            userId = prefs[UserPreferences.userId],
            genderIdentities = prefs[UserPreferences.genderIdentities].map { Gender.valueOf(it) },
            ageRangeMin = prefs[UserPreferences.ageRangeMin],
            ageRangeMax = prefs[UserPreferences.ageRangeMax],
            heightRangeMin = prefs[UserPreferences.heightRangeMin],
            heightRangeMax = prefs[UserPreferences.heightRangeMax],
            ethnicity = prefs[UserPreferences.ethnicities].map { Ethnicity.valueOf(it) },
            dateCities = cities,
            dateLanguages = prefs[UserPreferences.dateLanguages].map { Language.valueOf(it) },
            dateActivities = prefs[UserPreferences.dateActivities].map { Activity.valueOf(it) },
            dateLimit = prefs[UserPreferences.dateLimit],
            createdAt = prefs[UserPreferences.createdAt],
            updatedAt = prefs[UserPreferences.updatedAt]
        )
    }
}