package com.eros.users.service

import com.eros.common.errors.NotFoundException
import com.eros.database.dbQuery
import com.eros.users.models.City
import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.UpdatePreferenceRequest
import com.eros.users.models.UserPreference
import com.eros.users.repository.PreferenceRepository
import java.time.Clock
import java.time.Instant

class PreferenceService(
    private val preferenceRepository: PreferenceRepository,
    private val userService: UserService,
    private val clock: Clock = Clock.systemUTC()
) {

    suspend fun createPreferences(request: CreatePreferenceRequest): UserPreference {
        val now = Instant.now(clock)
        val preference = UserPreference(
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
                City(cityId = cityId, cityName = "",now,now)
            },
            reachLevel = request.reachLevel,
            createdAt = now,
            updatedAt = now
        )
        return preferenceRepository.create(preference)
    }

    suspend fun updatePreferences(userId: String, request: UpdatePreferenceRequest): UserPreference? {
        val now = Instant.now(clock)
        val preference = UserPreference(
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
                City(cityId = cityId, cityName = "",Instant.now(),Instant.now())
            },
            reachLevel = request.reachLevel,
            createdAt = now,
            updatedAt = now
        )
        return preferenceRepository.update(userId, preference)
    }

    /**
     * Function for finding out of another user matches a user's preference list.
     *
     * @param userId String of the user that's preferences will be used.
     * @param candidateId String of the user that will be tested.
     *
     * @return Boolean `true` if the candidate user matches the preferences of the user, otherwise `false`
     */
    suspend fun matchesUser(userId: String, candidateId: String): Boolean {
        // todo: Replace the entire user with a simple `UserMatchCandidate` dto? Less data wasted? Pass in User not id?
        val prefs = findByUserId(userId)
        val candidateProfile = userService.findByUserId(candidateId) ?:
            throw NotFoundException("User not found.")
        val candidatePreferences = findByUserId(candidateId)
        return prefs.matchesUser(candidateProfile, candidatePreferences)
    }

    /**
     * This function will check if two users both match each other preference list.
     *
     * @param userId String of one of the users
     * @param candidateId String of the id of the other user
     *
     * @return Boolean `true` if both profiles match the others preference list, otherwise `false`.
     */
    suspend fun usersBothMatchingPreferences(userId: String, candidateId: String): Boolean {
        // todo: Replace the entire user with a simple `UserMatchCandidate` dto? Less data wasted? Pass in User not id?
        val preferences = findByUserId(userId)
        val profile = userService.findByUserId(userId) ?: throw NotFoundException("User not found.")
        val candidateProfile = userService.findByUserId(candidateId)?:throw NotFoundException("User not found.")
        val candidatePreferences = findByUserId(candidateId)
        val matches1 = preferences.matchesUser(candidateProfile, candidatePreferences)
        val matches2 = candidatePreferences.matchesUser(profile, preferences)
        return matches1 && matches2
    }

    suspend fun estimateMatches() {
        //todo: Some sort of (sampling * size/samplesize) using the matchesUser?
    }

    /**
     * Finds a user's preferences by Firebase UID.
     *
     * @param userId Firebase user ID to search for
     * @return UserPreference if found, null otherwise
     */
    suspend fun findByUserId(userId: String): UserPreference {
        return preferenceRepository.getUserPreferenceWithCities(userId)
    }

    suspend fun doesExist(userId: String): Boolean {
        return preferenceRepository.userPreferencesDoesExist(userId)
    }

    /**
     * Function to delete the UserPreferences and the UserCitiesPreferences
     *
     * @return `1` if deleted otherwise `0`
     */
    suspend fun delete(userId: String): Int{
        return preferenceRepository.delete(userId)
    }
}
