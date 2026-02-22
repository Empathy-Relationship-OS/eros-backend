package com.eros.users.service

import com.eros.users.models.CreateUserCityPreferenceRequest
import com.eros.users.models.DeleteAllUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference
import com.eros.users.repository.UserCitiesRepository
import java.time.Clock
import java.time.Instant

class UserCitiesService(
    private val userCityRepository: UserCitiesRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    suspend fun createUserCityPreference(request: CreateUserCityPreferenceRequest): UserCityPreference {
        val entity = UserCityPreference(
            userId = request.userId,
            cityId = request.cityId,
            createdAt = Instant.now(clock)
        )
        return userCityRepository.addUserCityPreference(entity)
    }

    suspend fun addUserCityPreferencesBatch(userId: String, cityIds: List<Long>) {
        userCityRepository.addUserCityPreferencesBatch(userId, cityIds)
    }

    suspend fun deleteUserCityPreference(request: DeleteUserCityPreferenceRequest) {
        userCityRepository.deleteUserCityPreference(request)
    }

    suspend fun deleteAllUserCityPreference(request: DeleteAllUserCityPreferenceRequest) : Int{
        return userCityRepository.deleteAllUserCityPreference(request)
    }
}
