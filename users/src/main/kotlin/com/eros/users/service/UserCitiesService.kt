package com.eros.users.service

import com.eros.users.models.CreateUserCityPreferenceRequest
import com.eros.users.models.DeleteAllUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.repository.UserCitiesRepository

class UserCitiesService (private val userCityRepository : UserCitiesRepository) {

    suspend fun createUserCityPreference(request : CreateUserCityPreferenceRequest){
        userCityRepository.addUserCityPreference(request)
    }

    suspend fun addUserCityPreferencesBatch(userId: String, cityIds: List<Long>){
        userCityRepository.addUserCityPreferencesBatch(userId, cityIds)
    }

    suspend fun deleteUserCityPreference(request: DeleteUserCityPreferenceRequest){
        userCityRepository.deleteUserCityPreference(request)
    }


    suspend fun deleteAllUserCityPreference(request: DeleteAllUserCityPreferenceRequest){
        userCityRepository.deleteAllUserCityPreference(request)
    }
}