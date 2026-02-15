package com.eros.users.repository

import com.eros.users.models.CreateUserCityPreferenceRequest
import com.eros.users.models.DeleteUserCityPreferenceRequest
import com.eros.users.models.UserCityPreference

interface UserCitiesRepository {

    suspend fun addUserCityPreference(request : DeleteUserCityPreferenceRequest) : UserCityPreference

    suspend fun deleteUserCityPreference(cityId : Long, userId: String) : UserCityPreference?

}