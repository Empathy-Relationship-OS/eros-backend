package com.eros.users.repository

import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.UpdatePreferenceRequest
import com.eros.users.models.UserPreference

interface PreferenceRepository {

    suspend fun createPreferences(request : CreatePreferenceRequest) : UserPreference

    suspend fun updatePreference(preferenceId: Long, request: UpdatePreferenceRequest): UserPreference

    suspend fun getUserPreferenceWithCities(userId: String): UserPreference

}