package com.eros.users.repository

import com.eros.database.repository.IBaseDAO
import com.eros.users.models.UserPreference

interface PreferenceRepository : IBaseDAO<String, UserPreference> {

    /**
     * Retrieves a user's preferences joined with their city preferences.
     *
     * @param userId The Firebase UID of the user.
     * @return The [UserPreference] with [UserPreference.dateCities] populated.
     */
    suspend fun getUserPreferenceWithCities(userId: String): UserPreference

    suspend fun userPreferencesDoesExist(userId : String) : Boolean

    //fun delete(userId : String) : Int
}
