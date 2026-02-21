package com.eros.users.service

import com.eros.common.errors.NotFoundException
import com.eros.users.models.City
import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.UpdatePreferenceRequest
import com.eros.users.models.UserPreference
import com.eros.users.repository.PreferenceRepository
import com.eros.users.repository.UserRepositoryImpl
import java.time.Clock
import java.time.Instant

class PreferenceService(
    private val preferenceRepository: PreferenceRepository,
    private val userRepositoryImpl: UserRepositoryImpl = UserRepositoryImpl(),
    private val userService: UserService = UserService(userRepositoryImpl),
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
            reachLevel = request.reachLevel,
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
            reachLevel = request.reachLevel,
            createdAt = now,
            updatedAt = now
        )
        return preferenceRepository.update(preferenceId, preference)
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
        val candidatePreferences = findByUserId(candidateId) ?:
            throw NotFoundException("User's Preferences not found.")
        return prefs?.matchesUser(candidateProfile, candidatePreferences)
            ?: throw NotFoundException("User's Preferences not found.")
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
        val preferences = findByUserId(userId) ?: throw NotFoundException("User not found.")
        val profile = userService.findByUserId(userId) ?: throw NotFoundException("User's Preferences not found.")
        val candidateProfile = userService.findByUserId(candidateId)?:throw NotFoundException("User not found.")
        val candidatePreferences = findByUserId(candidateId)?:throw NotFoundException("User's Preferences not found.")
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
    suspend fun findByUserId(userId: String): UserPreference? {
        return preferenceRepository.getUserPreferenceWithCities(userId)
    }
}
