package com.eros.users.service

import com.eros.users.models.CreatePreferenceRequest
import com.eros.users.models.CreateUserRequest
import com.eros.users.models.UpdatePreferenceRequest
import com.eros.users.models.User
import com.eros.users.models.UserPreference
import com.eros.users.repository.PreferenceRepository
import com.eros.users.repository.UserRepository

class PreferenceService(private val preferenceRepository: PreferenceRepository) {

    suspend fun updatePreferences(preferenceId : Long, request: UpdatePreferenceRequest): UserPreference {
        return preferenceRepository.updatePreference(preferenceId, request)
    }

    suspend fun createPreferences(request: CreatePreferenceRequest): UserPreference {
        return preferenceRepository.createPreferences(request)
    }

    suspend fun estimateMatches(){}
    suspend fun matchesUser(){}

}