package com.eros.users.service

import com.eros.users.models.City
import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.UpdatePreferenceRequest
import com.eros.users.models.UserPreference
import com.eros.users.repository.PreferenceRepository
import java.time.Clock
import java.time.Instant

class PreferenceService(
    private val preferenceRepository: PreferenceRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    suspend fun createPreferences(request: CreatePreferenceRequest): UserPreference {
        val now = Instant.now(clock)
        val preference = UserPreference(
            id = 0L, // DB auto-generates the id on insert
            userId = request.userId,
            genderIdentities = request.genderIdentities,
            ageRangeMin = request.ageRangeMin,
            ageRangeMax = request.ageRangeMax,
            heightRangeMin = request.heightRangeMin,
            heightRangeMax = request.heightRangeMax,
            ethnicity = request.ethnicity,
            dateLanguages = request.dateLanguages,
            dateActivities = request.dateActivities,
            dateLimit = request.dateLimit,
            // Stub City objects carrying just the IDs the repo needs for its batch insert.
            // The full City data is populated by the repo via getUserPreferenceWithCities.
            dateCities = request.dateCities.map { cityId ->
                City(cityId = cityId, cityName = "", createdAt = now, updatedAt = now)
            },
            createdAt = now,
            updatedAt = now
        )
        return preferenceRepository.create(preference)
    }

    suspend fun updatePreferences(preferenceId: Long, request: UpdatePreferenceRequest): UserPreference? {
        val now = Instant.now(clock)
        val preference = UserPreference(
            id = request.id,
            userId = request.userId,
            genderIdentities = request.genderIdentities,
            ageRangeMin = request.ageRangeMin,
            ageRangeMax = request.ageRangeMax,
            heightRangeMin = request.heightRangeMin,
            heightRangeMax = request.heightRangeMax,
            ethnicity = request.ethnicity,
            dateLanguages = request.dateLanguages,
            dateActivities = request.dateActivities,
            dateLimit = request.dateLimit,
            // Stub City objects carrying just the IDs the repo needs for its batch insert.
            // The full City data is populated by the repo via getUserPreferenceWithCities.
            dateCities = request.dateCities.map { cityId ->
                City(cityId = cityId, cityName = "", createdAt = now, updatedAt = now)
            },
            createdAt = now,
            updatedAt = now
        )
        return preferenceRepository.update(preferenceId, preference)
    }

    suspend fun estimateMatches() {}
    suspend fun matchesUser() {}
}
